package com.luminary.access.application;

import com.luminary.access.domain.MembershipEntity;
import com.luminary.access.domain.MembershipRole;
import com.luminary.access.domain.TenantType;
import com.luminary.shared.identity.TenantId;

/**
 * Read view of an accessible workspace (E03-07). Only memberships the caller
 * can actually act in are listed.
 */
public record WorkspaceView(TenantId tenantId, String tenantName,
                            TenantType tenantType, MembershipRole role,
                            String status) {

    public static WorkspaceView from(MembershipEntity membership) {
        return new WorkspaceView(
                new TenantId(membership.getTenant().getId()),
                membership.getTenant().getName(),
                membership.getTenant().getType(),
                membership.getRole(),
                membership.getStatus().name());
    }
}