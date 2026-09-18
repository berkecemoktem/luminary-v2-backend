package com.luminary.access.api.dto;

import com.luminary.access.application.WorkspaceView;
import com.luminary.shared.identity.TenantId;

import java.util.List;

/**
 * Response body for {@code GET /api/v1/me}.
 */
public record MeResponse(MeUser user, TenantId activeWorkspace,
                         List<WorkspaceView> workspaces) {

    public record MeUser(String email, String displayName,
                         String country, String city) {
    }
}