package com.luminary.shared.port;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for reading a tenant's student membership roster. Implemented by the
 * {@code access} module's membership aggregate; returns plain data so module
 * boundaries stay clean.
 */
public interface StudentDirectoryPort {

    /**
     * Active student members of the given tenant, ordered by membership age
     * ascending (most recently joined last).
     */
    List<Member> members(String tenantId);

    /**
     * The active student membership of a user within the tenant, if any.
     */
    Optional<Member> member(String tenantId, UUID userId);

    record Member(UUID userId, String displayName, String email,
                  String country, String city, OffsetDateTime joinedAt) {
    }
}