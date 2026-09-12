package com.luminary.shared.identity;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier of a membership: the user-tenant-role authorization record.
 */
public record MembershipId(UUID value) {

    public MembershipId {
        Objects.requireNonNull(value, "value");
    }

    public static MembershipId fromString(String value) {
        return new MembershipId(UUID.fromString(value));
    }

    public String toString() {
        return value.toString();
    }
}