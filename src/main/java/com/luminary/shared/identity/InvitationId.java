package com.luminary.shared.identity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier of an invitation. Invitations are single-use, time limited
 * and bind a target role within a tenant.
 */
public record InvitationId(UUID value) {

    public InvitationId {
        Objects.requireNonNull(value, "value");
    }

    public static InvitationId fromString(String value) {
        return new InvitationId(UUID.fromString(value));
    }

    @JsonValue
    public String text() {
        return value.toString();
    }

    @JsonCreator
    public static InvitationId fromJson(String value) {
        return fromString(value);
    }

    public String toString() {
        return value.toString();
    }
}