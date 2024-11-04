package com.gmail.rahulpal21.cosmospaginator.buffer;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.SqlQuerySpec;
import com.google.common.reflect.TypeToken;
import org.junit.jupiter.api.AfterAll;
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

    @BeforeEach
    public void setupEach() {
        paginationBuffer = new TokenCachingPaginationBuffer<>(new PaginationRingBuffer<>(10, new TypeToken<List<TestData>>(getClass()) {}.getRawType()),container, querySpec, 10, TestData.class);
    }

    @Test
    void hasNext() {
        assertTrue(paginationBuffer.hasNext());
        List<TestData> page = (List<TestData>) paginationBuffer.next().toList();
        assertEquals(10, page.size());
        assertEquals("1", page.getFirst().getId());
        assertEquals("10", page.getLast().getId());
    }

    @Test
    void hasPrev() {
        assertFalse(paginationBuffer.hasPrev());
    }

    @Test
    void next() {
    }

    @Test
    void prev() {
    }

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

        //create collection
        String pkeyPath = "/id";
        if (database.createContainer(testCollectionName, pkeyPath).getStatusCode() != 201) {
            throw new RuntimeException("test collection could not be created");
        }
        container = database.getContainer(testCollectionName);
        //load test data
        AtomicInteger sequence = new AtomicInteger(1);
        createItems(container, sequence, 100);
    }

    private static void createItems(CosmosContainer container, AtomicInteger sequence, int count) {
        for (int i = 0; i < count; i++) {
            container.createItem(new TestData(String.valueOf(sequence.getAndIncrement())));
        }
    }

    @AfterAll
    public static void tearDown() {
        cosmosClient.getDatabase(testDBName).delete();
        cosmosClient.close();
    }

}