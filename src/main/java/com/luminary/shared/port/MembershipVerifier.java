package com.luminary.shared.port;

/**
 * Port for verifying membership and role within a tenant. Contract is
 * agnostic of any domain module so it lives in {@code shared}: implemented
 * by the {@code access} module and consumed by modules that need to check
 * the caller's authorization without importing access internals.
 */
public interface MembershipVerifier {

    /**
     * Verifies the user has an active membership in the given tenant and
     * returns the role name. Throws if not found, inactive, or tenant
     * is disabled.
     */
    String requireActiveRole(String tenantId, String userId);

    /**
     * Verifies the user has an active membership in the given tenant.
     * Throws if not found, inactive, or tenant is disabled.
     */
    void requireActiveMembership(String tenantId, String userId);

    /**
     * Returns true if the given role string represents an institution admin.
     */
    boolean isInstitutionAdmin(String role);
}