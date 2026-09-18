package com.luminary.shared.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SortCriterion(
        @NotBlank String field,
        @NotNull SortDirection direction) {
}
