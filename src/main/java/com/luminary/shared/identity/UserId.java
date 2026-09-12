package com.luminary.shared.identity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier of a global user. Identity is global; a user may hold
 * several memberships across tenants.
 */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "value");
    }

    public static UserId fromString(String value) {
        return new UserId(UUID.fromString(value));
    }

    @JsonValue
    public String text() {
        return value.toString();
    }

    @JsonCreator
    public static UserId fromJson(String value) {
        return fromString(value);
    }

    public String toString() {
        return value.toString();
    }
}