package com.gmail.rahulpal21.cosmospaginator;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.FeedResponse;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.google.common.collect.EvictingQueue;
import com.google.common.reflect.TypeToken;

import java.util.Iterator;
import java.util.stream.Stream;

public class CosmosPaginationBuilder<T> {

    private final CosmosContainer container;
    private final SqlQuerySpec querySpec;
    private int pageSize = 10; //TODO move to default properties or constants
    private int cacheSize = 10; //TODO move to default properties or constants
    private final Class<? super T> type = new TypeToken<T>() {
    }.getRawType();
    private Iterator<? extends FeedResponse<? super T>> pages;

    public CosmosPaginationBuilder(CosmosContainer container, SqlQuerySpec querySpec) {
        this.container = container;
        this.querySpec = querySpec;
    }

    public CosmosPaginationContext<T> buildAndQuery() {

        CosmosPagedIterable<? super T> pagedIterable = container.queryItems(querySpec, new CosmosQueryRequestOptions(), type);


        return new CosmosPaginationContext<T>() {
            private Iterator<? extends FeedResponse<? super T>> pages = pagedIterable.iterableByPage(pageSize).iterator();
            private EvictingQueue<T[]> ringBuffer = EvictingQueue.create(cacheSize);

            @Override
            public boolean hasNext() {
                return pages.hasNext();
            }

            @Override
            public boolean hasPrev() {
                return false;
            }

            @Override
            public Stream<? super T> getNextPage() {
                if(!hasNext()) {
                    return Stream.empty();
                }
                T[] pageData = (T[]) pages.next().getElements().stream().toArray();
                ringBuffer.offer(pageData);
                return Stream.of(pageData);
            }

            @Override
            public Stream<T> getPrevPage() {
                if(ringBuffer.isEmpty()){
                    return Stream.empty();
                }
                return Stream.of(ringBuffer.poll());
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
