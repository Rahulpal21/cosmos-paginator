import com.gmail.rahulpal21.cache.IPaginationRingBuffer;
import com.gmail.rahulpal21.cache.PaginationRingBuffer;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PaginationRingBufferTests {
    private static IPaginationRingBuffer<String> subject;
    private static int testSize = 10;

    @BeforeAll
    public static void setup() {
        subject = new PaginationRingBuffer<>(testSize);
    }

    @Test
    @Order(0)
    public void testSize() {
        assertEquals(testSize, subject.getLength());
    }

    @Test
    @Order(1)
    public void testReadOperationsOnEmptyBuffer() {
        assertNull(subject.peekNext());
        assertNull(subject.peekPrev());
        assertNull(subject.readNext());
        assertNull(subject.readPrev());
    }

    @Test
    @Order(2)
    public void testForwardOperations() {
        String entry1 = "First";
        String entry2 = "Second";
        String entry3 = "Third";
        String entry4 = "Fourth";
        String entry5 = "Fifth";

        subject.offerNext("First");
        assertEquals(entry1, subject.peekPrev());
        assertNull(subject.peekNext());
        assertEquals(entry1, subject.readPrev());
        assertNull(subject.peekPrev());
        assertNull(subject.readPrev());
        assertEquals(entry1, subject.peekNext());
        assertEquals(entry1, subject.readNext());
    }
}
