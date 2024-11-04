package com.gmail.rahulpal21.cosmospaginator.buffer;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.FeedResponse;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.gmail.rahulpal21.cosmospaginator.CosmosPaginable;
import com.google.common.reflect.TypeToken;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Stream;

public class TokenCachingPaginationBuffer<T> implements CosmosPaginable<T> {
    private final Class<? super T> type;
    private final IPaginationRingBuffer<List<T>> paginationBuffer;
    private final Deque<String> tokenRestoreStack;
    private final Stack<String> tokenStack;
    private final CosmosContainer container;
    private final SqlQuerySpec querySpec;
    private Iterator<FeedResponse<T>> pageIterator;
    private CosmosPagedIterable<T> cosmosPagedIterable;


    private int pageSize;

    public TokenCachingPaginationBuffer(IPaginationRingBuffer<List<T>> paginationBuffer, CosmosContainer container, SqlQuerySpec querySpec, int pageSize, Class type) {
        this.type = type;
        this.paginationBuffer = paginationBuffer;
        this.tokenRestoreStack = new LinkedBlockingDeque<>(paginationBuffer.getLength());
        this.tokenStack = new Stack<>();
        this.container = container;
        this.querySpec = querySpec;
        this.pageSize = pageSize;
        init();
    }

    private void init() {
        cosmosPagedIterable = (CosmosPagedIterable<T>) container.queryItems(querySpec, new CosmosQueryRequestOptions(), type);
        pageIterator = cosmosPagedIterable.iterableByPage(pageSize).iterator();
    }

    @Override
    public boolean hasNext() {
        if (paginationBuffer.peekNext() != null) {
            return true;
        }
        return pageIterator.hasNext();
    }

    @Override
    public boolean hasPrev() {
        if (paginationBuffer.peekPrev() != null) {
            return true;
        }
        return !tokenStack.isEmpty();
    }

    @Override
    public Stream<? super T> next() {
        if (paginationBuffer.peekNext() != null) {
            tokenStack.push(tokenRestoreStack.pop());
            return paginationBuffer.readNext().stream();
        }

        if (!tokenStack.isEmpty()) {
            pageIterator = cosmosPagedIterable.iterableByPage(tokenStack.pop(), pageSize).iterator();
        } else {
            pageIterator = cosmosPagedIterable.iterableByPage(pageSize).iterator();
        }

        if (pageIterator.hasNext()) {
            try {
                FeedResponse<T> next = pageIterator.next();
                List<T> elements = paginationBuffer.offerNext(next.getElements().stream().toList());
                tokenStack.push(next.getContinuationToken());
                return elements.stream();
            } catch (AllPagesNotReadException e) {
                throw new RuntimeException(e);
            }
        }
        return Stream.empty();
    }

    @Override
    public Stream<? super T> prev() {
        if (paginationBuffer.peekPrev() != null) {
            String token = tokenStack.pop();
            try {
                tokenRestoreStack.push(token);
            } catch (IllegalStateException stackFull) {
                tokenRestoreStack.pop();
                tokenRestoreStack.push(token);
            }
            return paginationBuffer.readPrev().stream();
        }
        if (!tokenStack.isEmpty()) {
            pageIterator = cosmosPagedIterable.iterableByPage(tokenStack.pop(), pageSize).iterator();
            try {
                FeedResponse<T> next = pageIterator.next();
                tokenStack.push(next.getContinuationToken());
                return paginationBuffer.offerPrev(next.getElements().stream().toList()).stream();
            } catch (AllPagesNotReadException e) {
                throw new RuntimeException(e);
            }
        }
        return Stream.empty();
    }

    @Override
    public long getPageCount() {
        throw new RuntimeException("Method not implemented");
    }

    @Override
    public void reset() {
        throw new RuntimeException("Method not implemented");
    }
}