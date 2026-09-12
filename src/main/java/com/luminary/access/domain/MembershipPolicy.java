package com.luminary.access.domain;

import com.luminary.shared.error.ApiException;

import java.util.Optional;

/**
 * Pure authorization rules applied by application services. Membership is the
 * single source of truth for authorization; the student role additionally has
 * ownership checks that are enforced per resource.
 */
public final class MembershipPolicy {

    private MembershipPolicy() {
    }

    public static MembershipEntity assertActive(MembershipEntity membership,
                                                String operation) {
        if (membership == null) {
            throw ApiException.forbidden(
                    "membership-not-found",
                    "No membership exists for this operation: " + operation);
        }
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw ApiException.forbidden(
                    "membership-inactive",
                    "Membership is not active: " + operation);
        }
        if (membership.getTenant().getStatus() != TenantStatus.ACTIVE) {
            throw ApiException.forbidden(
                    "tenant-inactive",
                    "Workspace is not active: " + operation);
        }
        return membership;
    }

    public static Optional<MembershipEntity> requireRole(
            Optional<MembershipEntity> membership, MembershipRole role,
            String operation) {
        MembershipEntity active = assertActive(
                membership.orElse(null), operation);
        if (active.getRole() != role) {
            throw ApiException.forbidden(
                    "role-required",
                    "Role " + role + " required for: " + operation);
        }
        return Optional.of(active);
    }
}