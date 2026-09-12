package com.luminary.access.api.dto;

import com.luminary.access.domain.MembershipRole;

/**
 * Body for creating an invitation: {@code POST /api/v1/invitations}.
 */
public record CreateInvitationRequest(String email, MembershipRole role) {
}