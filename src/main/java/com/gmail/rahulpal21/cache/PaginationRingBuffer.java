package com.gmail.rahulpal21.cache;

import com.google.common.reflect.TypeToken;

import java.lang.reflect.Array;

public class PaginationRingBuffer<T> implements IPaginationRingBuffer<T> {
    private final T[] ringBuffer;
    Class<? super T> type = new TypeToken<T>(getClass()) {
    }.getRawType();
    private final int length;
    private final int endIndex;

    /**
     * writeCursor (CW) starts from 0 and moves in forward direction one place on every call to offerNext()
     * CW switches to H on first call to offerPrev().
     */
//    private int writeCursor;

    /**
     * head (H) stays at the start of the (array) buffer until overwrite occurs in the forward direction.
     * On overwrite, H starts moving with writeCursor (CW) one position on every write in forward direction.
     * Overwrite is marked by movement of CW from N-1 to 0 the first time.
     */
    private int head;

    /**
     * readCursor (CR) starts from 0 and moves forward along with CW on each write in forward direction.
     * CR moves one place in the backward direction on every call to readPrev() until it reaches H.
     * CR moves one place in the forward direction on every call to reaNext() if it is not currently at CW.
     */
    private int cursor;
    private int tail;

    public PaginationRingBuffer(int length) {
        this.ringBuffer = (T[]) Array.newInstance(type, length);
        this.length = ringBuffer.length;
        this.endIndex = this.length - 1;
        this.cursor = 0;
        this.head = 0;
        this.tail = 0;
    }

    @Override
    public T offerNext(T element) throws AllPagesNotReadException {
        // add the element at head and increment head & cursor. set head to 0 if current position is N-1 where N is the length of the array.
        // if cursor is not equal to head, throw AllPagesNotReadException
        if (!isReadCursorAtHead()) {
            throw new AllPagesNotReadException("cursor: " + cursor + " head: " + head);
        }
        ringBuffer[head] = element;
        if (head == endIndex) {
            head = 0;
        } else {
            head++;
        }
        cursor = head;
        return element;
    }

    @Override
    public T offerPrev(T element) throws AllPagesNotReadException {
        // add the element at T and decrements T.
        // sets CR to T, and sets H to T-1
        // if T is currently set to 0, T is set to N-1
        // if CR is not currently at T, throw AllPagesNotReadException
        if (!isReadCursorAtTail()) {
            throw new AllPagesNotReadException("cursor: " + cursor + " tail: " + tail);
        }

        ringBuffer[tail] = element;
        if (tail == 0) {
            tail = endIndex;
        } else {
            tail--;
        }
        cursor = tail;
        return element;
    }

    @Override
    public T peekNext() {
        return cursor == head ? null : ringBuffer[cursor];
    }

    @Override
    public T peekPrev() {
        return cursor == tail ? null : ringBuffer[cursor - 1];
    }

    @Override
    public T readNext() {
        if (cursor == head) {
            return null;
        }
        T element = ringBuffer[cursor];
        cursor = cursor == length - 1 ? 0 : ++cursor;
        return element;
    }

    @Override
    public T readPrev() {
        if (cursor == tail) {
            return null;
        }
        if (cursor == 0) {
            cursor = length - 1;
        } else {
            cursor--;
        }
        return ringBuffer[cursor];
    }

    @Override
    public int getLength() {
        return ringBuffer.length;
    }

    private boolean isReadCursorAtHead() {
        return cursor == head;
    }

    private boolean isReadCursorAtTail() {
        return cursor == tail;
    }
}
