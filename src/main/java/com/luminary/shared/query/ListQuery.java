package com.luminary.shared.query;

import java.util.List;

/**
 * Server-driven list request, serialized from the web client's
 * {@code SearchRequest} contract: {@code skip, take, filters, sorts}.
 */
public record ListQuery(Integer skip, Integer take,
                        List<FilterCriterion> filters,
                        List<SortCriterion> sorts) {

    public int skipOrDefault() {
        return skip == null ? 0 : Math.max(0, skip);
    }

    public int takeOrDefault() {
        return take == null ? 20 : take;
    }

    public List<FilterCriterion> filtersOrEmpty() {
        return filters == null ? List.of() : filters;
    }

    public List<SortCriterion> sortsOrEmpty() {
        return sorts == null ? List.of() : sorts;
    }
}