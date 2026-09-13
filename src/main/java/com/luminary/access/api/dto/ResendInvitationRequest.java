package com.luminary.access.api.dto;

/**
 * Body for re-sending an invitation:
 * {@code POST /api/v1/invitations/resend}.
 */
public record ResendInvitationRequest(String email) {
}