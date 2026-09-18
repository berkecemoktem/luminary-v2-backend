package com.luminary.shared.query;

/**
 * Operations supported by the shared list-query contract. A resource decides
 * which subset is valid for each exposed field.
 */
public enum FilterOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    STARTS_WITH,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    IN,
    BETWEEN,
    IS_NULL,
    IS_NOT_NULL
}
