package com.luminary.shared.query;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Common request body for searchable table/list endpoints.
 */
public record SearchRequest(
        @Min(0) Integer skip,
        @Min(1) @Max(100) Integer take,
        @Valid @Size(max = 10) List<FilterCriterion> filters,
        @Valid @Size(max = 3) List<SortCriterion> sorts) {

    public static final int DEFAULT_SKIP = 0;
    public static final int DEFAULT_TAKE = 20;

    public SearchRequest {
        skip = skip == null ? DEFAULT_SKIP : skip;
        take = take == null ? DEFAULT_TAKE : take;
        filters = filters == null ? List.of() : List.copyOf(filters);
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
    }
}
