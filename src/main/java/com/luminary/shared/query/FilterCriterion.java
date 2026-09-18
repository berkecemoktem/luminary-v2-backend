package com.luminary.shared.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * One client-supplied filter. Scalar operators use {@code value}; collection
 * operators ({@code IN} and {@code BETWEEN}) use {@code values}.
 */
public record FilterCriterion(
        @NotBlank String field,
        @NotNull FilterOperator operator,
        Object value,
        @Size(max = 100) List<Object> values) {

    public FilterCriterion {
        values = values == null ? List.of() : List.copyOf(values);
    }
}
