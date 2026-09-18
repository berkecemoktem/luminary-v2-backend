package com.luminary.access.infrastructure.adapter;

import com.luminary.access.domain.MembershipEntity;
import com.luminary.access.domain.MembershipRole;
import com.luminary.access.domain.MembershipStatus;
import com.luminary.access.domain.TenantStatus;
import com.luminary.access.infrastructure.MembershipRepository;
import com.luminary.shared.error.ApiException;
import com.luminary.shared.port.MembershipVerifier;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter that implements the student module's {@link MembershipVerifier}
 * port using access-module internals.
 */
@Component
public class MembershipVerifierAdapter implements MembershipVerifier {

    private final MembershipRepository membershipRepository;

    public MembershipVerifierAdapter(
            MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    public String requireActiveRole(String tenantId, String userId) {
        MembershipEntity membership = membershipRepository
                .findByTenantIdAndUserId(tenantId, UUID.fromString(userId))
                .orElseThrow(() -> ApiException.forbidden(
                        "membership-not-found",
                        "You are not a member of this workspace."));

        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw ApiException.forbidden("membership-revoked",
                    "Your membership has been revoked.");
        }
        if (membership.getTenant().getStatus() != TenantStatus.ACTIVE) {
            throw ApiException.forbidden("tenant-inactive",
                    "This workspace is inactive.");
        }
        return membership.getRole().name();
    }

    @Override
    public void requireActiveMembership(String tenantId, String userId) {
        MembershipEntity membership = membershipRepository
                .findByTenantIdAndUserId(tenantId, UUID.fromString(userId))
                .orElseThrow(() -> ApiException.forbidden(
                        "membership-not-found",
                        "You are not a member of this workspace."));

        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw ApiException.forbidden("membership-revoked",
                    "Your membership has been revoked.");
        }
        if (membership.getTenant().getStatus() != TenantStatus.ACTIVE) {
            throw ApiException.forbidden("tenant-inactive",
                    "This workspace is inactive.");
        }
    }

    @Override
    public boolean isInstitutionAdmin(String role) {
        return MembershipRole.INSTITUTION_ADMIN.name().equals(role);
    }
}
