import com.gmail.rahulpal21.cache.AllPagesNotReadException;
import com.gmail.rahulpal21.cache.IPaginationRingBuffer;
import com.gmail.rahulpal21.cache.PaginationRingBuffer;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;


//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PaginationRingBufferTests {
    private static IPaginationRingBuffer<String> buffer;
    private final int testSize = 10;
    private static final String DATA_PREFIX = "data";

    @BeforeEach
    public void setup() {
        buffer = new PaginationRingBuffer<>(testSize);
    }

    @Test
//    @Order(0)
    public void testSize() {
        assertEquals(testSize, buffer.getLength());
    }

    @Test
//    @Order(1)
    public void testReadOperationsOnEmptyBuffer() {
        assertNull(buffer.peekNext());
        assertNull(buffer.peekPrev());
        assertNull(buffer.readNext());
        assertNull(buffer.readPrev());
    }

    @Test
//    @Order(2)
    public void testForwardOperations() {
        String entry1 = "First";

        try {
            buffer.offerNext(entry1);
        } catch (AllPagesNotReadException e) {
            throw new RuntimeException(e);
        }
        assertEquals(entry1, buffer.peekPrev());
        assertNull(buffer.peekNext());
        assertEquals(entry1, buffer.readPrev());
        assertNull(buffer.peekPrev());
        assertNull(buffer.readPrev());
        assertEquals(entry1, buffer.peekNext());
        assertEquals(entry1, buffer.readNext());
    }

    // during offerNext(), head keeps incrementing in forward direction and tail remains fixed at start of array until overflow
    @Test
//    @Order(3)
    public void testHeadMovesForwardAndTailRemainsFixedUntilOverwriteDuringOfferNext() {
        assertNull(buffer.peekNext());
        assertNull(buffer.peekPrev());

        fill(buffer, 1,9, DATA_PREFIX);
        assertNull(buffer.peekNext());
        for (int i = 9; i > 0; i--) {
            assertEquals(DATA_PREFIX + i, buffer.readPrev());
        }
        assertNull(buffer.readPrev());
    }

    // peekNext() & readNext() should not read into tail when buffer is full
    @Test
//    @Order(4)
    public void testReadForwardOperationsDoNotReadIntoTailWhenBufferIsFull(){
        fill(buffer,1,10,DATA_PREFIX);
        assertNull(buffer.peekNext());
        assertNull(buffer.readNext());
    };

    // peekPrev() & readPrev() should not read into head when buffer is full
    @Test
//    @Order(5)
    public void testReadBackwardOperationsDoNotReadIntoHeadWhenBufferIsFull(){
        fill(buffer,1,10,DATA_PREFIX);
        skipBackward(buffer, 10);
        assertNull(buffer.peekPrev());
        assertNull(buffer.readPrev());
    }

    // during offerNext(), head jumps to 0 from N-1 when overflow occurs. tail starts shifting one position in forward direction
    @Test
//    @Order(5)
    public void testOfferNextStartsOverwritingFromStartOfBuffer(){
        fill(buffer,1,10,DATA_PREFIX);
        assertEquals(DATA_PREFIX+10,buffer.peekPrev());

        fill(buffer, 11,1,DATA_PREFIX);
        assertEquals(DATA_PREFIX+11,buffer.peekPrev());
        skipBackward(buffer,9);
        assertEquals(DATA_PREFIX+2, buffer.readPrev());
        assertNull(buffer.peekPrev());
        assertNull(buffer.readPrev());
    }

    // during offerNext(), once in overwriting mode, tail keeps moving in forward direction on every write

    // during offerPrev(), tail jumps from 0 to N-1

    // during offerPrev(), tail moves in backward direction on every write. head remains at current position until overwrite occurs

    // offerNext() throws AllPagesNotReadException if all pages are not read in forward direction

    // offerPrev() throws AllPagesNotReadException if all pages are not read in backward direction

    // peekNext() always reads the element at cursor without moving the cursor

    // peekPrev() always reads the element at cursor-1 without moving the cursor

    // readNext() reads the element at cursor and moves cursor forward one position

    // readNext() moves cursor forward one position only until it reaches head, at which point it starts returning null and does not move the cursor

    // during readNext(), cursor jumps from N-1 to 0 if head is not end of buffer

    // readNext() stops at head even when head is positioned at left of the cursors start position

    // readPrev() reads the element at cursor-1 and moves cursor backward one position

    // readPrev() moves cursor backward one position only until it reaches tail, at which point it starts returning null and does not move the cursor

    // during readPrev(), cursor jumps from 0 to N-1 if tail is not start of buffer

    // readPrev() stops at tail even when tail is positioned at right of the cursors start position

    private void fill(IPaginationRingBuffer buffer, int startSequence, int elementCount, String prefix) {
        for (int i = 0; i < elementCount; i++) {
            try {
                buffer.offerNext(new String(prefix + startSequence++));
            } catch (AllPagesNotReadException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void skipBackward(IPaginationRingBuffer<String> buffer, int skipCount) {
        for (int i = 0; i < skipCount; i++) {
            buffer.readPrev();
        }
    }
}
