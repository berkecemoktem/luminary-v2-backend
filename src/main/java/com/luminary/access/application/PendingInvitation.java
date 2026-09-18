package com.luminary.access.application;

import com.luminary.access.domain.MembershipRole;
import com.luminary.shared.identity.InvitationId;
import com.luminary.shared.identity.TenantId;

import java.time.OffsetDateTime;

/**
 * A still-valid invitation addressed to the signed-in user, shown in the
 * in-app notification center. Accepting happens through the same single-use
 * link stored in {@code acceptUrl}.
 */
public record PendingInvitation(InvitationId invitationId, TenantId tenantId,
                                String tenantName, MembershipRole role,
                                OffsetDateTime expiresAt, String acceptUrl) {
}