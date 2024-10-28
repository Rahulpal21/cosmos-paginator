package com.gmail.rahulpal21.cache;

import com.google.common.reflect.TypeToken;
import java.lang.reflect.Array;

public class PaginationRingBuffer<T> {
    private final T[] ringBuffer;
    Class<? super T> type = new TypeToken<T>() {}.getRawType();
    private final int length;
    private int cursor;
    private int head;

    public PaginationRingBuffer(int length) {
        this.length = length;
        ringBuffer = (T[]) Array.newInstance(type, this.length);
    }

    public T offerNext(){
        return null;
    }

    public T offerPrev(){
        return null;
    }

    public T peekNext(){
        return null;
    }

    public T peekPrev(){
        return null;
    }

    public T readNext(){
        return null;
    }

    public T readPrev(){
        return null;
    }

}
