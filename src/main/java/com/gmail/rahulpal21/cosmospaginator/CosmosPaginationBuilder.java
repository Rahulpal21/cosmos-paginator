package com.gmail.rahulpal21.cosmospaginator;

import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.SqlQuerySpec;

import java.util.stream.Stream;

public class CosmosPaginationBuilder<T> {

    private final CosmosAsyncContainer container;
    private final SqlQuerySpec querySpec;

    public CosmosPaginationBuilder(CosmosAsyncContainer container, SqlQuerySpec querySpec) {
        this.container = container;
        this.querySpec = querySpec;
    }

    public CosmosPaginationContext<T> build(){
        return new CosmosPaginationContext<T>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public boolean hasPrev() {
                return false;
            }

            @Override
            public Stream<T> getNextPage() {
                return Stream.empty();
            }

            @Override
            public Stream<T> getPrevPage() {
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
}
