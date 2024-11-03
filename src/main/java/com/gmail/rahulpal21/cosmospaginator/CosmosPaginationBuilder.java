package com.gmail.rahulpal21.cosmospaginator;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.FeedResponse;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.gmail.rahulpal21.cosmospaginator.buffer.AllPagesNotReadException;
import com.gmail.rahulpal21.cosmospaginator.buffer.IPaginationRingBuffer;
import com.google.common.collect.EvictingQueue;
import com.google.common.reflect.TypeToken;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.stream.Stream;

public class CosmosPaginationBuilder<T> {

    private int pageSize = 10; //TODO move to default properties or constants
    private int cacheSize = 10; //TODO move to default properties or constants
    private final Class<? super T> type = new TypeToken<T>(getClass()) {
    }.getRawType();

    public CosmosPaginationContext<T> build(IPaginationRingBuffer<List<T>> paginationCache, CosmosContainer container, SqlQuerySpec querySpec) {

        CosmosPagedIterable<T> pagedIterable = (CosmosPagedIterable<T>) container.queryItems(querySpec, new CosmosQueryRequestOptions(), type);

        return new CosmosPaginationContext<T>() {
            private CosmosContainer icontainer = container;
            private SqlQuerySpec iquerySpec = querySpec;
            private Iterator<FeedResponse<T>> pageIteraor = pagedIterable.iterableByPage(pageSize).iterator();
            private IPaginationRingBuffer<List<T>> ipaginationCache = paginationCache;
            private Stack<String> tokens = new Stack<>();
            private EvictingQueue<String> tempTokens = EvictingQueue.create(paginationCache.getLength());

            private boolean checkHasNextThroughCache() {
                if (ipaginationCache.peekNext() != null) {
                    return true;
                }
                return pageIteraor.hasNext();
            }

            private boolean checkHasPrevThroughCache() {
                if (ipaginationCache.peekPrev() != null) {
                    return true;
                }
                return false;
            }

            private FeedResponse<T> enqueueToken(FeedResponse<T> page) {
                tokens.push(page.getContinuationToken());
                return page;
            }

            private List<T> saveTokenToTemp(List<T> page) {
                tempTokens.offer(tokens.pop());
                return page;
            }

            @Override
            public boolean hasNext() {
                return checkHasNextThroughCache();
            }

            @Override
            public boolean hasPrev() {
                return checkHasPrevThroughCache();
            }

            @Override
            public Stream<T> next() {
                if (ipaginationCache.peekNext() != null) {
                    return ipaginationCache.readNext().stream();
                }

                if (pageIteraor.hasNext()) {
                    try {
                        return ipaginationCache.offerNext(enqueueToken(pageIteraor.next()).getElements().stream().toList()).stream();
                    } catch (AllPagesNotReadException e) {
                        //TODO log error
                        throw new RuntimeException(e);
                    }
                }

                return Stream.empty();
            }

            @Override
            public Stream<T> prev() {
                if(tokens.empty()){
                    return Stream.empty();
                }
                if (paginationCache.peekPrev() != null) {
                    return saveTokenToTemp(paginationCache.readPrev()).stream();
                }
//                container.
//                paginationCache.offerPrev()
                return Stream.empty();
            }

            @Override
            public long getPageCount() {
                return 0;
            }

            @Override
            public void reset() {

            }
        };
    }

    public CosmosPaginationBuilder setPageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }
}
