package com.luminary.shared.identity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * Typed identifier of a tenant (institutional or personal workspace). Tenants
 * are the data boundary of every tenant-scoped record; the identifier is a
 * unique, human-friendly string (e.g. {@code "sinav-ankara"}), not a UUID.
 */
public record TenantId(String value) {

    public TenantId {
        Objects.requireNonNull(value, "value");
    }

    public static TenantId fromString(String value) {
        return new TenantId(value);
    }

    @JsonValue
    public String text() {
        return value;
    }

    @JsonCreator
    public static TenantId fromJson(String value) {
        return fromString(value);
    }

    public String toString() {
        return value;
    }
}