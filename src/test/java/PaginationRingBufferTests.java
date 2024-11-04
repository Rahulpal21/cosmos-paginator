import com.gmail.rahulpal21.cosmospaginator.buffer.AllPagesNotReadException;
import com.gmail.rahulpal21.cosmospaginator.buffer.IPaginationRingBuffer;
import com.gmail.rahulpal21.cosmospaginator.buffer.PaginationRingBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PaginationRingBufferTests {
    private static IPaginationRingBuffer<String> buffer;
    private final int testSize = 10;
    private static final String DATA_PREFIX = "data";

    @BeforeEach
    public void setup() {
        buffer = new PaginationRingBuffer<>(testSize, String.class);
    }

    @Test
//    @Order(0)
    public void testSize() {
        assertEquals(testSize, buffer.getLength());
    }

    @Test
    public void testReadOperationsOnEmptyBuffer() {
        assertNull(buffer.peekNext());
        assertNull(buffer.peekPrev());
        assertNull(buffer.readNext());
        assertNull(buffer.readPrev());
    }

    @Test
    public void testOperationsWithSingleEntry() {
        String entry1 = "First";

        try {
            buffer.offerNext(entry1);
        } catch (AllPagesNotReadException e) {
            throw new RuntimeException(e);
        }
        assertNull(buffer.peekPrev());
        assertNull(buffer.peekNext());
        assertNull(buffer.readPrev());
        assertNull(buffer.peekNext());

    }

    // during offerNext(), head keeps incrementing in forward direction and tail remains fixed at start of array until overflow
    @Test
    public void testHeadMovesForwardAndTailRemainsFixedUntilOverwriteDuringOfferNext() {
        assertNull(buffer.peekNext());
        assertNull(buffer.peekPrev());

        fill(buffer, 1, 9, DATA_PREFIX);
        assertNull(buffer.peekNext());
        for (int i = 8; i > 0; i--) {
            assertEquals(DATA_PREFIX + i, buffer.peekPrev());
            assertEquals(DATA_PREFIX + i, buffer.readPrev());
        }
        //cursor should be positioned at TAIL at this point
        assertNull(buffer.peekPrev());
        assertNull(buffer.readPrev());
    }

    // during offerNext(), head keeps incrementing in forward direction and tail remains fixed at start of array until overflow
    @Test
    public void testForwardWriteTillTheLastElementOfArray() {
        assertNull(buffer.peekNext());
        assertNull(buffer.peekPrev());

        fill(buffer, 1, 10, DATA_PREFIX);

        //cursor should be positioned at HEAD at this point
        assertNull(buffer.peekNext());
        assertNull(buffer.readNext());

        //backward read should go all the way to the start of the array where TAIL should be positioned
        for (int i = 9; i > 0; i--) {
            assertEquals(DATA_PREFIX + i, buffer.peekPrev());
            assertEquals(DATA_PREFIX + i, buffer.readPrev());
        }
        //cursor should be positioned at TAIL at this point
        assertNull(buffer.peekPrev());
        assertNull(buffer.readPrev());
    }

    // peekNext() & readNext() should not read into tail when buffer is full
    @Test
    public void testReadForwardOperationsDoNotReadIntoTailWhenBufferIsFull() {
        fill(buffer, 1, 10, DATA_PREFIX);
        assertNull(buffer.peekNext());
        assertNull(buffer.readNext());
    }

    ;

    // peekPrev() & readPrev() should not read into head when buffer is full
    @Test
    public void testReadBackwardOperationsDoNotReadIntoHeadWhenBufferIsFull() {
        fill(buffer, 1, 10, DATA_PREFIX);
        skipBackward(buffer, 9);
        assertNull(buffer.peekPrev());
        assertNull(buffer.readPrev());
    }

    // during offerNext(), head jumps to 0 from N-1 when overflow occurs. tail starts shifting one position in forward direction
    @Test
    public void testOfferNextStartsOverwritingFromStartOfBuffer() {
        fill(buffer, 1, 10, DATA_PREFIX);
        assertEquals(DATA_PREFIX + 9, buffer.peekPrev());

        fill(buffer, 11, 1, DATA_PREFIX);
        assertEquals(DATA_PREFIX + 10, buffer.peekPrev());
        skipBackward(buffer, 8);
        assertEquals(DATA_PREFIX + 2, buffer.readPrev());
        assertNull(buffer.peekPrev());
        assertNull(buffer.readPrev());
    }

    // during offerNext(), once in overwriting mode, tail keeps moving in forward direction on every write
    @Test
    public void testOfferNextKeepsShiftingTailWhenOverwriting() {
        int startSequence = 1;
        int elementCount;
        fill(buffer, startSequence, elementCount = 10, DATA_PREFIX);

        int elementsToReadBackToReachBeforeTail = 8;
        for (int i = 1; i <= 9; i++) {
            fill(buffer, startSequence += elementCount, elementCount = 1, DATA_PREFIX);
            skipBackward(buffer, elementsToReadBackToReachBeforeTail);
            assertEquals(DATA_PREFIX + (startSequence - (elementsToReadBackToReachBeforeTail + 1)), buffer.readPrev());
            assertNull(buffer.peekPrev());
            assertNull(buffer.readPrev());
            skipForward(buffer, elementsToReadBackToReachBeforeTail + 1);
        }
    }

    // during offerPrev(), tail jumps from 0 to N-1
    @Test
    public void testOfferPrevShiftsTailToEndOfBufferFrom0() {
        int numOfForwardWritesToPositionTailAtEndOfBuffer = 19;
        int startSequence = 1;
        fill(buffer, startSequence, numOfForwardWritesToPositionTailAtEndOfBuffer, DATA_PREFIX);

        //check if tail is positioned at end of buffer
        skipBackward(buffer, 8);
        assertEquals(DATA_PREFIX + 10, buffer.peekPrev());
        skipBackward(buffer, 1);
        assertNull(buffer.peekPrev());

        //force tail jump from N-1 to 0
        skipForward(buffer, 9);
        fill(buffer, startSequence += numOfForwardWritesToPositionTailAtEndOfBuffer, 1, DATA_PREFIX);

        //check if tail is positioned at start of buffer
        skipBackward(buffer, 8);
        assertEquals(DATA_PREFIX + 11, buffer.peekPrev());
        skipBackward(buffer, 1);
        assertNull(buffer.peekPrev());

    }

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

    private void skipForward(IPaginationRingBuffer<String> buffer, int skipCount) {
        for (int i = 0; i < skipCount; i++) {
            buffer.readNext();
        }
    }

}
