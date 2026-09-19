package com.luminary.shared.query;

/**
 * A paginated response, serialized with the same shape the web client's
 * {@code PageResponse} contract expects: {@code items, skip, take,
 * totalItems, hasNext}.
 */
public record Page<T>(java.util.List<T> items, int skip, int take,
                      long totalItems, boolean hasNext) {
}