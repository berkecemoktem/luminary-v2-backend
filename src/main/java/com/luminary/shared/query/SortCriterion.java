package com.luminary.shared.query;

/**
 * A single sort applied server-side: a field name plus ASC or DESC.
 */
public record SortCriterion(String field, String direction) {
}