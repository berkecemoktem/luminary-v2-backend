package com.luminary.shared.query;

/**
 * A single filter applied server-side. Compared against the list item (or an
 * underlying record) by field name; unsupported fields are ignored.
 */
public record FilterCriterion(String field, String operator, Object value) {
}