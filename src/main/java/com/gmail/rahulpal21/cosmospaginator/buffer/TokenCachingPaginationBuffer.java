package com.gmail.rahulpal21.cosmospaginator.buffer;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.FeedResponse;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.gmail.rahulpal21.cosmospaginator.CosmosPaginable;

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
    private String nextToken;
    private final CosmosContainer container;
    private final SqlQuerySpec querySpec;
    private Iterator<FeedResponse<T>> pageIterator;
    private CosmosPagedIterable<T> cosmosPagedIterable;


    private int pageSize;
    private boolean continuityBreak;

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
        nextToken = "";
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

        if(continuityBreak){
            pageIterator = cosmosPagedIterable.iterableByPage(nextToken,pageSize).iterator();
            continuityBreak = false;
        }
        if (pageIterator.hasNext()) {
            try {
                FeedResponse<T> next = pageIterator.next();
                List<T> elements = next.getElements().stream().toList();

                if (!elements.isEmpty()) {
                    paginationBuffer.offerNext(elements);

                    if(!tokenRestoreStack.isEmpty())tokenStack.push(tokenRestoreStack.pop());
                    tokenRestoreStack.push(nextToken);
                    nextToken = next.getContinuationToken();
                    return elements.stream();
                }
            } catch (AllPagesNotReadException e) {
                throw new RuntimeException(e);
            }
        }
        return Stream.empty();
    }

    @Override
    public Stream<? super T> prev() {
        if (paginationBuffer.peekPrev() != null) {
            saveToken();
            return paginationBuffer.readPrev().stream();
        }

        //Continuation token for the prev page to be read is always on the tokenStack
        //tokenRestoreStack always has the latest page read through next/prev
        if (!tokenStack.isEmpty()) {
            //first page is marked with an empty string token
            if(tokenStack.peek().isEmpty()){
                //first page
                pageIterator = cosmosPagedIterable.iterableByPage(pageSize).iterator();
            }else {
                pageIterator = cosmosPagedIterable.iterableByPage(tokenStack.peek(), pageSize).iterator();
            }

            try {
                FeedResponse<T> next = pageIterator.next();
                // cosmos return continuation token for the next page.
                // as the next continuation token is already present in tokenRestoreStack,
                // it's the token that is used to fetch the current page, that needs to be added and not
                // the one from current cosmos response.
                saveToken();
                return paginationBuffer.offerPrev(next.getElements().stream().toList()).stream();
            } catch (AllPagesNotReadException e) {
                throw new RuntimeException(e);
            }
        }
        return Stream.empty();
    }

    private void saveToken() {
        String token = tokenStack.pop();
        try {
            tokenRestoreStack.push(token);
        } catch (IllegalStateException stackFull) {
            //the stack should behave like an evicting queue,
            // hence removing the earliest token from stack and not the latest
            nextToken = tokenRestoreStack.removeLast();
            tokenRestoreStack.push(token);
            continuityBreak = true;
        }
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