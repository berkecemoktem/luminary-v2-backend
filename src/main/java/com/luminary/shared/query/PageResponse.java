package com.luminary.shared.query;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable response envelope shared by paginated endpoints.
 */
public record PageResponse<T>(
        List<T> items,
        long skip,
        int take,
        long totalItems,
        boolean hasNext) {

    public PageResponse {
        items = List.copyOf(items);
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        long offset = page.getPageable().getOffset();
        return new PageResponse<>(
                page.getContent(),
                offset,
                page.getPageable().getPageSize(),
                page.getTotalElements(),
                offset + page.getNumberOfElements()
                        < page.getTotalElements());
    }
}
