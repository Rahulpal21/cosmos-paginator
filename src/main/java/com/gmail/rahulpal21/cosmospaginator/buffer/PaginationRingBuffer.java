package com.gmail.rahulpal21.cosmospaginator.buffer;

import com.google.common.reflect.TypeToken;

import java.lang.reflect.Array;

public class PaginationRingBuffer<T> implements IPaginationRingBuffer<T> {
    private final T[] ringBuffer;
    Class<? super T> type;
    private final int length;
    private final int endIndex;

    /**
     * head (H) moves one position forward on each call to offerNext till it reaches last element of array at index (N-1) where N is length of array
     * Next call to offerNext switches H to start of the array at index 0 and starts to shift tail (T) one position forward.
     * H moves one position in the backward direction on each call to offerPrev when buffer is full.
     */
    private int head;

    /**
     * If T is at a non-zero position, it keeps moving one position in the backward direction on each call to offerPrev. T switches to N-1 if it is positioned at 0 and a call to offerPrev is made.
     * When the buffer is full, offerPrev starts shifting H one position backward.
     */
    private int tail;

    /**
     * cursor (C) is always positioned to the last read (readPrev or readNext) or last written (offerNext or offerPrev) element.
     * C moves one position forward on each call to readNext until it reaches H, at which point all subsequent calls to readNext return null unless new elements are added in forward direction with offerNext. Each call to offerNext moves C (along with H) forward one position.
     * C moves one position backward on each call to readPrev until it reaches T, at which point all subsequent calls to readPrev return null unless new elements are added to the buffer in backward direction with offerPrev.Each call to offerPrev moves C (along with T) backward one position.
     */
    private int cursor;


    public PaginationRingBuffer(int length, Class<? super T> type) {
        this.type = type;
        this.ringBuffer = (T[]) Array.newInstance(type, length);
        this.length = ringBuffer.length;
        this.endIndex = this.length - 1;
        this.cursor = -1;
        this.head = -1;
        this.tail = -1;
    }

    @Override
    public synchronized T offerNext(T element) throws AllPagesNotReadException {
        // throw AllPagesNotReadException if C is not currently positioned at H.
        // move H one position forward (switch to 0 if currently at N-1) and add the element at new H. Also move C one position forward to position at H.
        if (!isReadCursorAtHead()) {
            throw new AllPagesNotReadException("cursor: " + cursor + " head: " + head);
        }

        if (head == -1) {
            head = tail = cursor = 0;
        } else if (head == endIndex) {
            head = cursor = 0;
            tail++;//assuming tail is at 0
        } else {
            if (tail == head + 1) {
                if (tail == endIndex) {
                    tail = 0;
                } else {
                    tail++;
                }
            }
            cursor = ++head;
        }
        return ringBuffer[head] = element;
    }

    @Override
    public synchronized T offerPrev(T element) throws AllPagesNotReadException {
        // throw AllPagesNotReadException if C is not currently positioned at T.
        // move T one position backward (switch to N-1 if currently at 0) and add the element at new T. Also move C to match T.
        if (!isReadCursorAtTail()) {
            throw new AllPagesNotReadException("cursor: " + cursor + " tail: " + tail);
        }

        if (tail == -1) {
            tail = head = cursor = 0;
        } else if (tail == 0) {
            tail = cursor = endIndex;
            if (head == endIndex) {
                head--;
            }
        } else {
            if (head == tail - 1) {
                if (head == 0) {
                    head = endIndex;
                } else {
                    head--;
                }
            }
            cursor = --tail;
        }
        return ringBuffer[tail] = element;
    }

    @Override
    public T peekNext() {
        return cursor == head ? null : ringBuffer[cursor == endIndex ? 0 : cursor + 1];
    }

    @Override
    public T peekPrev() {
        return cursor == tail ? null : ringBuffer[cursor == 0 ? endIndex : cursor - 1];
    }

    @Override
    public synchronized T readNext() {
        return isReadCursorAtHead() ? null : ringBuffer[cursor == endIndex ? cursor = 0 : ++cursor];
    }

    @Override
    public synchronized T readPrev() {
        return isReadCursorAtTail() ? null : ringBuffer[cursor == 0 ? cursor = endIndex : --cursor];
    }

    @Override
    public T readCurrent() {
        return cursor != -1 ? ringBuffer[cursor] : null;
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
