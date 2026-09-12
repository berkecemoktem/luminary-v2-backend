package com.luminary.access.application;

import com.luminary.access.application.port.CurrentSession;
import com.luminary.access.domain.InvitationEntity;
import com.luminary.access.domain.MembershipEntity;
import com.luminary.access.domain.MembershipRole;
import com.luminary.access.domain.MembershipStatus;
import com.luminary.access.domain.TenantEntity;
import com.luminary.access.domain.TenantType;
import com.luminary.access.domain.UserEntity;
import com.luminary.access.infrastructure.InvitationRepository;
import com.luminary.access.infrastructure.MembershipRepository;
import com.luminary.access.infrastructure.UserRepository;
import com.luminary.shared.error.ApiException;
import com.luminary.shared.identity.InvitationId;
import com.luminary.shared.identity.TenantId;
import com.luminary.shared.identity.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * Single-use, time-limited invitations (E03-08). Only the admin of a tenant
 * may invite; accepting binds the account whose verified OIDC email matches
 * the invited email.
 */
@Service
public class InvitationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final InvitationRepository invitationRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final CurrentSession currentSession;
    private final AccessAppProperties properties;

    public InvitationService(
            InvitationRepository invitationRepository,
            MembershipRepository membershipRepository,
            UserRepository userRepository,
            CurrentSession currentSession,
            AccessAppProperties properties) {
        this.invitationRepository = invitationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.currentSession = currentSession;
        this.properties = properties;
    }

    @Transactional
    public InvitationCreated create(UUID actorUserId, TenantId tenantId,
                                    String invitedEmail,
                                    MembershipRole role) {
        MembershipEntity actor = requireActiveMembership(actorUserId,
                tenantId);
        if (actor.getRole() != MembershipRole.INSTITUTION_ADMIN) {
            throw ApiException.forbidden("role-required-institution-admin",
                    "Only an institution admin can invite users.");
        }
        TenantEntity tenant = actor.getTenant();
        if (tenant.getType() != TenantType.INSTITUTION) {
            throw ApiException.conflict("invitation-not-supported",
                    "Invitations are only available for institutions.");
        }

        String email = normalizeEmail(invitedEmail);
        if (email == null || email.isBlank()) {
            throw ApiException.badRequest("email-required",
                    "A valid email is required.");
        }
        MembershipRole targetRole =
                role == null ? MembershipRole.STUDENT : role;

        invitationRepository
                .findByTenantIdAndEmailAndUsedAtIsNull(tenant.getId(), email)
                .ifPresent(existing -> {
                    throw ApiException.conflict("invitation-already-pending",
                            "An active invitation already exists for this email.");
                });

        String token = randomToken();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plus(properties.getInvitationTtl());

        InvitationEntity invitation = InvitationEntity.create(
                tenant, email, targetRole, sha256(token), expiresAt,
                actorUserId);
        invitationRepository.save(invitation);

        String acceptUrl = properties.getInvitationAcceptUrlTemplate()
                .replace("{token}", token);

        return new InvitationCreated(
                new InvitationId(invitation.getId()), tenantId, email,
                targetRole, token, expiresAt, acceptUrl);
    }

    @Transactional
    public InvitedWorkspace accept(UUID actorUserId, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw ApiException.badRequest("token-required",
                    "An invitation token is required.");
        }

        InvitationEntity invitation = invitationRepository
                .findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> ApiException.notFound(
                        "invitation-not-found",
                        "This invitation does not exist."));

        if (invitation.isUsed()) {
            throw ApiException.conflict("invitation-already-used",
                    "This invitation has already been used.");
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (invitation.isExpired(now)) {
            throw ApiException.conflict("invitation-expired",
                    "This invitation has expired.");
        }

        UserEntity user = userRepository.findById(actorUserId)
                .orElseThrow(() -> ApiException.forbidden("user-not-found",
                        "Account not found."));
        String accountEmail = normalizeEmail(user.getEmail());
        if (!invitation.getEmail().equals(accountEmail)) {
            throw ApiException.forbidden("invitation-account-mismatch",
                    "This invitation is for a different email address.");
        }

        TenantEntity tenant = invitation.getTenant();
        if (membershipRepository
                .existsByTenantIdAndUserId(tenant.getId(), actorUserId)) {
            throw ApiException.conflict("already-member",
                    "You are already a member of this workspace.");
        }
        if (membershipRepository
                .findByUserIdAndStatus(actorUserId, MembershipStatus.ACTIVE)
                .stream()
                .anyMatch(m -> m.getTenant().getType()
                        == TenantType.INSTITUTION)) {
            throw ApiException.conflict("active-institution-exists",
                    "You already have an active institution workspace.");
        }

        MembershipEntity membership = membershipRepository.save(
                MembershipEntity.create(tenant, user,
                        invitation.getRole()));
        invitation.markUsed(now);
        invitationRepository.save(invitation);

        TenantId joinedTenant = new TenantId(tenant.getId());
        currentSession.authenticate(new UserId(actorUserId), joinedTenant);

        return new InvitedWorkspace(
                joinedTenant, tenant.getName(),
                tenant.getType(), invitation.getRole(),
                membership.getJoinedAt());
    }

    private MembershipEntity requireActiveMembership(UUID userId,
                                                     TenantId tenantId) {
        MembershipEntity membership = membershipRepository
                .findByTenantIdAndUserId(tenantId.value(), userId)
                .orElseThrow(() -> ApiException.notFound(
                        "membership-not-found",
                        "You are not a member of this workspace."));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw ApiException.forbidden("membership-revoked",
                    "Your membership in this workspace has been revoked.");
        }
        return membership;
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}