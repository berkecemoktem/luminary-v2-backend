package com.luminary.access.api.dto;

/**
 * Body for accepting an invitation: {@code POST /api/v1/invitations/accept}.
 */
public record AcceptInvitationRequest(String token) {
}