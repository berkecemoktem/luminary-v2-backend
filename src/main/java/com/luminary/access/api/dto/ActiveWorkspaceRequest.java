package com.luminary.access.api.dto;

import com.luminary.shared.identity.TenantId;

/**
 * Body for switching the active workspace: {@code POST /api/v1/me/active-workspace}.
 */
public record ActiveWorkspaceRequest(TenantId tenantId) {
}