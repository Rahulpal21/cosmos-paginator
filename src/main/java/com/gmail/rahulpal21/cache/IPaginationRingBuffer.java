package com.gmail.rahulpal21.cache;

public interface IPaginationRingBuffer<T> {
    T offerNext(T element);
    T offerPrev(T element);
    T peekNext();
    T peekPrev();
    T readNext();
    T readPrev();
    int getLength();
}
