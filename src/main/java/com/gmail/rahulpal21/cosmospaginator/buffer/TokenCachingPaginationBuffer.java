package com.gmail.rahulpal21.cosmospaginator.buffer;

import com.azure.cosmos.models.FeedResponse;
import com.google.common.collect.EvictingQueue;

import java.util.List;
import java.util.Stack;

public class TokenCachingPaginationBuffer<T> implements IPaginationRingBuffer<FeedResponse<T>>{
    private final IPaginationRingBuffer<List<T>> paginationBuffer;
    private final EvictingQueue<String> readAheadTokens;
    private final Stack<String> tokenStack;

    public TokenCachingPaginationBuffer(IPaginationRingBuffer<List<T>> paginationBuffer) {
        this.paginationBuffer = paginationBuffer;
        this.readAheadTokens = EvictingQueue.create(paginationBuffer.getLength());
        this.tokenStack = new Stack<>();
    }

    @Override
    public FeedResponse<T> offerNext(FeedResponse<T> element) throws AllPagesNotReadException {
        paginationBuffer.offerNext(element.getElements().stream().toList());
        tokenStack.push(element.getContinuationToken());
        return element;
    }

    @Override
    public FeedResponse<T> offerPrev(FeedResponse<T> element) throws AllPagesNotReadException {
        paginationBuffer.offerPrev(element.getElements().stream().toList());
        return null;
    }

    @Override
    public FeedResponse<T> peekNext() {
        return null;
    }

    @Override
    public FeedResponse<T> peekPrev() {
        return null;
    }

    @Override
    public FeedResponse<T> readNext() {
        return null;
    }

    @Override
    public FeedResponse<T> readPrev() {
        return null;
    }

    @Override
    public FeedResponse<T> readCurrent() {
        return null;
    }

    @Override
    public int getLength() {
        return 0;
    }
}