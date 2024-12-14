package com.gmail.rahulpal21.cosmospaginator.buffer;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.SqlQuerySpec;
import com.google.common.reflect.TypeToken;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/// These tests depend on a cosmos instance or emulator provided through application-junit.xml
class TokenCachingPaginationBufferTest {
    private static final String propertiesFile = "application-junit.yaml";
    private static CosmosClient cosmosClient;
    private static String endpoint;
    private static String accessKey;
    private static String testDBName;
    private static String testCollectionName;
    private SqlQuerySpec querySpec = new SqlQuerySpec("SELECT * FROM c");
    private TokenCachingPaginationBuffer<TestData> paginationBuffer;
    private static CosmosContainer container;

    @BeforeAll
    public static void setup() {
        //read properties
        Yaml yaml = new Yaml();
        Map<String, Object> properties;
        try (InputStream ioStream = TokenCachingPaginationBufferTest.class.getClassLoader().getResourceAsStream(propertiesFile)) {
            properties = yaml.load(ioStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //extract cosmos connection properties
        Map<String, String> cosmosProp = (Map<String, String>) ((Map) properties.get("junit")).get("cosmos");
        endpoint = cosmosProp.get("url");
        accessKey = cosmosProp.get("key");
        testDBName = cosmosProp.get("testDBName");
        testCollectionName = cosmosProp.get("testCollectionName");

        //establish connection
        cosmosClient = new CosmosClientBuilder().endpoint(endpoint).key(accessKey).buildClient();

        //cleanup if older db exists
        CosmosDatabase database = cosmosClient.getDatabase(testDBName);
        try {
            database.delete();
        } catch (Exception e) {

        }

        //create new test db
        if (cosmosClient.createDatabase(testDBName).getStatusCode() == 201) {
            database = cosmosClient.getDatabase(testDBName);
        } else {
            throw new RuntimeException("test database could not be created");
        }
    }

    @BeforeEach
    public void setupEach() {
        //cleanup if older db exists
        CosmosDatabase database = cosmosClient.getDatabase(testDBName);

        //recreate collection if already exists
        try {
            database.getContainer(testCollectionName).delete();
        } catch (Exception e) {
        }

        //create collection
        String pkeyPath = "/id";
        if (database.createContainer(testCollectionName, pkeyPath).getStatusCode() != 201) {
            throw new RuntimeException("test collection could not be created");
        }
        container = database.getContainer(testCollectionName);
    }

    @Test
    void hasNext() {
        createAndInitializeBuffer(100);

        assertTrue(paginationBuffer.hasNext());
        List<TestData> page = (List<TestData>) paginationBuffer.next().toList();
        assertEquals(10, page.size());
        assertEquals("1", page.getFirst().getId());
        assertEquals("10", page.getLast().getId());

        //should return false when all pages are consumed
        for (int i = 0; i < 9; i++) {
            //out of 10 pages, one is read already, remaining 9 are read in this loop
            assertTrue(paginationBuffer.hasNext());
            assertEquals(10, paginationBuffer.next().toList().size());
        }

        //there should be no more pages
        assertFalse(paginationBuffer.hasNext());
    }

    @Test
    void hasPrev() {
        createAndInitializeBuffer(100);

        assertFalse(paginationBuffer.hasPrev());

        //move the cursor forward a few positions to allow backward navigation
        int positions = 5;
        for (int i = 0; i < 5; i++) {
            paginationBuffer.next();
        }

        //should read the penultimate page to the last written/read page in forward direction
        positions--;
        for (int i = positions; i > 0; i--) {
            assertTrue(paginationBuffer.hasPrev());
            List<TestData> page = (List<TestData>) paginationBuffer.prev().toList();
            assertEquals(10, page.size());
            assertEquals(String.valueOf((i * 10) - 9), page.getFirst().getId());
            assertEquals(String.valueOf(i * 10), page.getLast().getId());
        }

        assertFalse(paginationBuffer.hasPrev());
    }

    @Test
    void testNavigationWithBufferOverflow() {
        createAndInitializeBuffer(150);

        //traverse all the way to last page (would require 15 calls to next)
        //should return false when all pages are consumed
        for (int i = 1; i <= 15; i++) {
            assertTrue(paginationBuffer.hasNext());
            List<TestData> page = (List<TestData>) paginationBuffer.next().toList();
            assertEquals(10, page.size());
            assertEquals(String.valueOf((i * 10) - 9), page.getFirst().getId());
            assertEquals(String.valueOf(i * 10), page.getLast().getId());
        }
        //no more elements in forward direction
        assertFalse(paginationBuffer.hasNext());
        assertTrue(paginationBuffer.next().toList().isEmpty());

        //now traversing all the way back to first page (should need 14 calls to prev)
        for (int i = 14; i > 0; i--) {
            assertTrue(paginationBuffer.hasPrev());
            List<TestData> page = (List<TestData>) paginationBuffer.prev().toList();
            assertEquals(10, page.size());
            assertEquals(String.valueOf((i * 10) - 9), page.getFirst().getId());
            assertEquals(String.valueOf(i * 10), page.getLast().getId());
        }
        //no more elements in backward direction
        assertFalse(paginationBuffer.hasPrev());
        assertTrue(paginationBuffer.prev().toList().isEmpty());

        // now it should be possible to navigate in forward direction again.
        for (int i = 2; i <= 15; i++) {
            assertTrue(paginationBuffer.hasNext());
            List<TestData> page = (List<TestData>) paginationBuffer.next().toList();
            assertEquals(10, page.size());
            assertEquals(String.valueOf((i * 10) - 9), page.getFirst().getId());
            assertEquals(String.valueOf(i * 10), page.getLast().getId());
        }

        //no more elements in forward direction
        assertFalse(paginationBuffer.hasNext());
        assertTrue(paginationBuffer.next().toList().isEmpty());

    }

    @Test
    void testSwitchingBetweenForwardAndBackwardWithoutFullTraversal() {
        createAndInitializeBuffer(100);

        //traverse forward mid-way
        for (int i = 1; i <= 5; i++) {
            assertTrue(paginationBuffer.hasNext());
            List<TestData> page = (List<TestData>) paginationBuffer.next().toList();
            assertEquals(10, page.size());
            assertEquals(String.valueOf((i * 10) - 9), page.getFirst().getId());
            assertEquals(String.valueOf(i * 10), page.getLast().getId());
        }

        //now traversing backward few places
        for (int i = 4; i > 2; i--) {
            assertTrue(paginationBuffer.hasPrev());
            List<TestData> page = (List<TestData>) paginationBuffer.prev().toList();
            assertEquals(10, page.size());
            assertEquals(String.valueOf((i * 10) - 9), page.getFirst().getId());
            assertEquals(String.valueOf(i * 10), page.getLast().getId());
        }

        // now it should be possible to navigate in forward direction again till the end.
        for (int i = 4; i <= 10; i++) {
            assertTrue(paginationBuffer.hasNext());
            List<TestData> page = (List<TestData>) paginationBuffer.next().toList();
            assertEquals(10, page.size());
            assertEquals(String.valueOf((i * 10) - 9), page.getFirst().getId());
            assertEquals(String.valueOf(i * 10), page.getLast().getId());
        }

        //no more elements in forward direction
        assertFalse(paginationBuffer.hasNext());
        assertTrue(paginationBuffer.next().toList().isEmpty());

    }

    @Test
    void next() {

    }

    @Test
    void prev() {
    }

    private void createAndInitializeBuffer(int testDatasetSize) {
        //load test data in container
        AtomicInteger sequence = new AtomicInteger(1);
        createItems(container, sequence, testDatasetSize);

        paginationBuffer = new TokenCachingPaginationBuffer<>(new PaginationRingBuffer<>(10, new TypeToken<List<TestData>>(getClass()) {
        }.getRawType()), container, querySpec, 10, TestData.class);
    }

    private static void createItems(CosmosContainer container, AtomicInteger sequence, int count) {
        for (int i = 0; i < count; i++) {
            container.createItem(new TestData(String.valueOf(sequence.getAndIncrement())));
        }
    }

    //    @AfterAll
    public static void tearDown() {
        cosmosClient.getDatabase(testDBName).delete();
        cosmosClient.close();
    }

}