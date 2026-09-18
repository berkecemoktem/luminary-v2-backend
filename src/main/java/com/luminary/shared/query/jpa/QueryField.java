package com.luminary.shared.query.jpa;

import com.luminary.shared.query.FilterOperator;

import java.util.EnumSet;
import java.util.Set;

/**
 * Safe mapping from a public query field to an entity attribute path.
 */
public record QueryField(
        String publicName,
        String entityPath,
        Class<?> valueType,
        boolean sortable,
        Set<FilterOperator> operators) {

    public QueryField {
        if (publicName == null || publicName.isBlank()) {
            throw new IllegalArgumentException("Public field name is required");
        }
        if (entityPath == null || entityPath.isBlank()) {
            throw new IllegalArgumentException("Entity path is required");
        }
        if (valueType == null) {
            throw new IllegalArgumentException("Field value type is required");
        }
        operators = operators == null || operators.isEmpty()
                ? Set.of()
                : Set.copyOf(EnumSet.copyOf(operators));
    }
}
