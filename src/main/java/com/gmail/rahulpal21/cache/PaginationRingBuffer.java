package com.gmail.rahulpal21.cache;

import com.google.common.reflect.TypeToken;

import java.lang.reflect.Array;

public class PaginationRingBuffer<T> implements IPaginationRingBuffer<T> {
    private final T[] ringBuffer;
    Class<? super T> type = new TypeToken<T>(getClass()) {
    }.getRawType();
    private final int length;
    private int cursor;
    private int head;

    public PaginationRingBuffer(int length) {
        this.ringBuffer = (T[]) Array.newInstance(type, length);
        this.length = ringBuffer.length;
        this.cursor = 0;
        this.head = 0;
    }

    @Override
    public T offerNext(T element) {
        ringBuffer[cursor] = element;

        return null;
    }

    @Override
    public T offerPrev(T element) {
        return null;
    }

    @Override
    public T peekNext() {
        return null;
    }

    @Override
    public T peekPrev() {
        return null;
    }

    @Override
    public T readNext() {
        return null;
    }

    @Override
    public T readPrev() {
        return null;
    }

    @Override
    public int getLength() {
        return ringBuffer.length;
    }

}
