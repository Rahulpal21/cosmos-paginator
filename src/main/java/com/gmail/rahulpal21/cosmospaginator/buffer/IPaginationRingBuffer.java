package com.gmail.rahulpal21.cosmospaginator.buffer;

/**
 * TODO explain the high level concept and the allowed navigations with this structure
 * Acronyms used in this documentation:
 * H head
 * T tail
 * C cursor position
 * @param <T>
 */
public interface IPaginationRingBuffer<T> {
    /**
     * adds the provided element in the buffer at the next slot.
     * If the buffer is full, next call to offerNext() starts overwriting the elements from the tail.
     * offerNext() throws AllPagesNotReadException if readPrev() is invoked and subsequent calls to
     * readNext() not done to read all the cached entries in the forward direction.
     * @param element data to be added to buffer
     * @return returns back the added element
     * @throws AllPagesNotReadException
     */
    T offerNext(T element) throws AllPagesNotReadException;

    /**
     * inserts the provided element in the buffer at the tail.
     * This operation would overwrite the element at the head and moves the head in backward direction
     * if the buffer is full.
     * This method throws AllPagesNotReadException if readPrev() is not invoked to navigate through the
     * whole buffer and bring the cursor to the tail.
     * @param element returns back the inserted element
     * @return data to be added to buffer
     * @throws AllPagesNotReadException
     */
    T offerPrev(T element) throws AllPagesNotReadException;

    /**
     * Return the element at the current CR position. Does not change the cursor position.
     * Returns null if CR = CW, i.e. the cursor is positioned at the head.
     * @return data element of type [T] or null
     */
    T peekNext();

    /**
     * Return the element at CR-1 position. Does not change the cursor position.
     * Returns null if CR = T, i.e. the cursor is positioned at the tail.
     * @return data element of type [T] or null
     */
    T peekPrev();

    /**
     * Return the element at the current CR position and position the cursor CR at the next slot in buffer.
     * Returns null if CR = CW, i.e. the cursor is positioned at the head.
     * @return data element of type [T] or null
     */
    T readNext();

    /**
     * Return the element at CR-1 position and re-position the cursor at the read slot.
     * Returns null if CR = T, i.e. the cursor is positioned at the tail.
     * @return data element of type [T] or null
     */
    T readPrev();

    T readCurrent();

    /**
     * returns length/size of the buffer. i.e, the maximum number of elements buffer can hold before it starts overwriting.
     * @return length of the internal buffer
     */
    int getLength();
}
