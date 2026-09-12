package com.luminary.access.application;

import com.luminary.access.domain.MembershipRole;
import com.luminary.shared.identity.InvitationId;
import com.luminary.shared.identity.TenantId;

import java.time.OffsetDateTime;

/**
 * Result of creating an invitation. The raw single-use token is returned
 * exactly once; only its SHA-256 hash is persisted.
 */
public record InvitationCreated(InvitationId invitationId, TenantId tenantId,
                                String email, MembershipRole role,
                                String token, OffsetDateTime expiresAt,
                                String acceptUrl) {
}