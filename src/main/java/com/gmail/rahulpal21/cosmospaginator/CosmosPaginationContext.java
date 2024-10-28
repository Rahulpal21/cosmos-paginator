package com.gmail.rahulpal21.cosmospaginator;

import java.util.stream.Stream;

/// @author Rahul Pal
/// Defines the api for consuming cosmos query responses with pagination. It provides provdes methods for navigating pages in either direction.
public interface CosmosPaginationContext<T> {
    ///@return true if more pages exist in forward direction
    boolean hasNext();
    ///@return true if more pages exist in backward direction
    boolean hasPrev();
    ///@return A [java.util.stream] instance to consume elements of type [T] from the returned page
    Stream<? super T> getNextPage();
    ///@return A [java.util.stream] instance to consume elements of type [T] from the returned page
    Stream<? super T> getPrevPage();
    ///@return Total number of pages
    long getPageCount();
    /// Resets the context to first page
    void reset();
}
