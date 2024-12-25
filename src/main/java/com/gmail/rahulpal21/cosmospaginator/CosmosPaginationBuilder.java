package com.gmail.rahulpal21.cosmospaginator;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.SqlQuerySpec;
import com.gmail.rahulpal21.cosmospaginator.buffer.PaginationRingBuffer;
import com.gmail.rahulpal21.cosmospaginator.buffer.TokenCachingPaginationBuffer;
import com.google.common.reflect.TypeToken;

import java.util.List;

public class CosmosPaginationBuilder<T> {

    private int pageSize = 10; //TODO move to default properties or constants
    private int cacheSize = 10; //TODO move to default properties or constants

    private final Class<? super T> type = new TypeToken<T>(getClass()) {
    }.getRawType();

    public CosmosPaginationBuilder() {
    }

    public CosmosPaginable<T> build(CosmosContainer container, SqlQuerySpec querySpec, Class<? extends T> dataType) {
        PaginationRingBuffer<List<T>> paginationCache = new PaginationRingBuffer<>(cacheSize, List.class);
        return new TokenCachingPaginationBuffer<>(paginationCache, container, querySpec, pageSize, dataType);
    }

    public CosmosPaginationBuilder setPageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public CosmosPaginationBuilder setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
        return this;
    }
}
