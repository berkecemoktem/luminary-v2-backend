package com.luminary.access.application;

import com.luminary.access.domain.MembershipRole;
import com.luminary.access.domain.TenantType;
import com.luminary.shared.identity.TenantId;

import java.time.OffsetDateTime;

/**
 * Result of accepting an invitation: the workspace membership just created.
 */
public record InvitedWorkspace(TenantId tenantId, String tenantName,
                               TenantType tenantType, MembershipRole role,
                               OffsetDateTime joinedAt) {
}