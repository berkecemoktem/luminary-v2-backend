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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Single-use, time-limited invitations (E03-08). Only the admin of a tenant
 * may invite or resend; accepting binds the account whose verified OIDC email
 * matches the invited email and creates the {@link MembershipEntity}.
 *
 * The bearer token is a presigned-URL-style secret: it is generated with high
 * entropy, never persisted (only its SHA-256 hash is stored), is valid for a
 * limited time and can be used once. It travels to the invited student only
 * inside the invitation email and is never returned to the inviting admin.
 */
@Service
public class InvitationService {

    private static final Logger log =
            LoggerFactory.getLogger(InvitationService.class);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final InvitationRepository invitationRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final CurrentSession currentSession;
    private final AccessAppProperties properties;
    private final InvitationMailer invitationMailer;
    private final SchoolNotificationService schoolNotificationService;

    public InvitationService(
            InvitationRepository invitationRepository,
            MembershipRepository membershipRepository,
            UserRepository userRepository,
            CurrentSession currentSession,
            AccessAppProperties properties,
            InvitationMailer invitationMailer,
            SchoolNotificationService schoolNotificationService) {
        this.invitationRepository = invitationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.currentSession = currentSession;
        this.properties = properties;
        this.invitationMailer = invitationMailer;
        this.schoolNotificationService = schoolNotificationService;
    }

    @Transactional
    public InvitationCreated create(UUID actorUserId, TenantId tenantId,
                                    String invitedEmail,
                                    MembershipRole role) {
        MembershipEntity actor = requireActiveMembership(actorUserId,
                tenantId);
        requireInstitutionAdmin(actor);
        TenantEntity tenant = actor.getTenant();

        String email = normalizeEmail(invitedEmail);
        if (email == null || email.isBlank()) {
            throw ApiException.badRequest("email-required",
                    "A valid email is required.");
        }
        MembershipRole targetRole =
                role == null ? MembershipRole.STUDENT : role;

        invitationRepository
                .findByTenantIdAndEmailAndUsedAtIsNullAndRejectedAtIsNull(
                        tenant.getId(), email)
                .ifPresent(existing -> {
                    throw ApiException.conflict("invitation-already-pending",
                            "An active invitation already exists for this "
                                    + "email.");
                });

        return createInvitationAndEmail(actor, email, targetRole);
    }

    /**
     * Re-sends an invitation. Because only the hash of the previous token is
     * stored (the raw secret was delivered by email and is not retrievable),
     * a resend always retires the previous unresolved link and issues a fresh
     * token. A previously rejected invitation stays rejected.
     */
    @Transactional
    public InvitationCreated resend(UUID actorUserId, TenantId tenantId,
                                    String invitedEmail) {
        MembershipEntity actor = requireActiveMembership(actorUserId,
                tenantId);
        requireInstitutionAdmin(actor);

        String email = normalizeEmail(invitedEmail);
        if (email == null || email.isBlank()) {
            throw ApiException.badRequest("email-required",
                    "A valid email is required.");
        }

        InvitationEntity existing = invitationRepository
                .findFirstByTenantIdAndEmailOrderByCreatedAtDesc(
                        tenantId.value(), email)
                .orElseThrow(() -> ApiException.notFound(
                        "invitation-not-found",
                        "No invitation exists for this email."));
        if (existing.isUsed()) {
            throw ApiException.conflict("invitation-already-used",
                    "This invitation has already been accepted.");
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (!existing.isRejected()) {
            existing.markUsed(now);
            invitationRepository.save(existing);
        }

        return createInvitationAndEmail(actor, email,
                existing.getRole());
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
        rejectOtherPendingInstitutions(accountEmail, invitation.getId(), now);

        schoolNotificationService.recordAccepted(tenant,
                actorUserId, user.getEmail(), user.getDisplayName(),
                invitation.getId());

        TenantId joinedTenant = new TenantId(tenant.getId());
        currentSession.authenticate(new UserId(actorUserId), joinedTenant);

        return new InvitedWorkspace(
                joinedTenant, tenant.getName(),
                tenant.getType(), invitation.getRole(),
                membership.getJoinedAt());
    }

    /**
     * A user can hold at most one institution workspace, so accepting one
     * invitation invalidates every other still-open invitation on the same
     * email. Only unused, unrejected, not-yet-expired invitations are closed;
     * already-expired ones simply fall out of the pending list on their own.
     */
    private void rejectOtherPendingInstitutions(String email, UUID acceptedId,
                                                OffsetDateTime now) {
        List<InvitationEntity> otherPending = invitationRepository
                .findByEmailAndUsedAtIsNullAndRejectedAtIsNull(email)
                .stream()
                .filter(inv -> !inv.getId().equals(acceptedId))
                .filter(inv -> !inv.isExpired(now))
                .toList();
        for (InvitationEntity inv : otherPending) {
            inv.markRejected(now);
        }
        if (!otherPending.isEmpty()) {
            invitationRepository.saveAll(otherPending);
        }
    }

    /**
     * Declines a still-open invitation addressed to the signed-in user. The
     * invitation is removed from the pending surface and can be re-issued by
     * the inviting institution with a fresh link.
     */
    @Transactional
    public void reject(UUID actorUserId, InvitationId invitationId) {
        InvitationEntity invitation = invitationRepository
                .findById(invitationId.value())
                .orElseThrow(() -> ApiException.notFound(
                        "invitation-not-found",
                        "This invitation does not exist."));

        UserEntity user = userRepository.findById(actorUserId)
                .orElseThrow(() -> ApiException.forbidden("user-not-found",
                        "Account not found."));
        String accountEmail = normalizeEmail(user.getEmail());
        if (accountEmail == null
                || !invitation.getEmail().equals(accountEmail)) {
            throw ApiException.forbidden("invitation-account-mismatch",
                    "This invitation is for a different email address.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (invitation.isUsed()) {
            throw ApiException.conflict("invitation-already-used",
                    "This invitation has already been accepted.");
        }
        if (invitation.isRejected()) {
            throw ApiException.conflict("invitation-already-rejected",
                    "This invitation has already been rejected.");
        }
        if (invitation.isExpired(now)) {
            throw ApiException.conflict("invitation-expired",
                    "This invitation has expired.");
        }

        invitation.markRejected(now);
        invitationRepository.save(invitation);

        schoolNotificationService.recordRejected(invitation.getTenant(),
                actorUserId, user.getEmail(), user.getDisplayName(),
                invitation.getId());
    }

    private InvitationCreated createInvitationAndEmail(
            MembershipEntity actor, String email, MembershipRole role) {
        TenantEntity tenant = actor.getTenant();
        String token = randomToken();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plus(properties.getInvitationTtl());
        String acceptUrl = properties.getInvitationAcceptUrlTemplate()
                .replace("{token}", token);

        InvitationEntity invitation = invitationRepository.save(
                InvitationEntity.create(tenant, email, role,
                        sha256(token), expiresAt, acceptUrl,
                        actor.getUser().getId()));

        sendInvitationEmailAfterCommit(tenant, email, acceptUrl, expiresAt);

        return new InvitationCreated(
                new InvitationId(invitation.getId()), new TenantId(tenant.getId()),
                email, role, expiresAt);
    }

    /**
     * Pending invitations for the signed-in user, matched by their verified
     * email. Only un-used, not-yet-expired invitations that have a persisted
     * accept link are surfaced to the in-app notification center.
     */
    @Transactional(readOnly = true)
    public List<PendingInvitation> pendingFor(UUID actorUserId) {
        UserEntity user = userRepository.findById(actorUserId)
                .orElseThrow(() -> ApiException.forbidden("user-not-found",
                        "Account not found."));
        String email = normalizeEmail(user.getEmail());
        if (email == null) {
            return List.of();
        }
        OffsetDateTime now = OffsetDateTime.now();
        return invitationRepository
                .findByEmailAndUsedAtIsNullAndRejectedAtIsNull(email)
                .stream()
                .filter(inv -> inv.getAcceptUrl() != null)
                .filter(inv -> !inv.isExpired(now))
                .map(inv -> new PendingInvitation(
                        new InvitationId(inv.getId()),
                        new TenantId(inv.getTenant().getId()),
                        inv.getTenant().getName(), inv.getRole(),
                        inv.getExpiresAt(), inv.getAcceptUrl()))
                .toList();
    }

    /**
     * Delivers the invitation email only after the transaction that created
     * the invitation has committed, so a rolled-back invitation never leaks a
     * token. Called after commit inside Spring's transaction; when there is no
     * active transaction (for example direct unit-test calls) it is sent
     * immediately. A delivery failure is logged, never propagated: mail is a
     * side channel and must not undo the invitation record.
     */
    private void sendInvitationEmailAfterCommit(
            TenantEntity tenant, String email, String acceptUrl,
            OffsetDateTime expiresAt) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            deliverInvitationMail(tenant, email, acceptUrl,
                                    expiresAt);
                        }
                    });
        } else {
            deliverInvitationMail(tenant, email, acceptUrl, expiresAt);
        }
    }

    private void deliverInvitationMail(TenantEntity tenant, String email,
                                       String acceptUrl,
                                       OffsetDateTime expiresAt) {
        try {
            invitationMailer.sendInvitation(tenant, email, acceptUrl,
                    expiresAt);
        } catch (RuntimeException e) {
            log.error("Invitation email delivery failed for tenant={} to={}",
                    tenant.getId(), email, e);
        }
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

    private static void requireInstitutionAdmin(MembershipEntity actor) {
        if (actor.getRole() != MembershipRole.INSTITUTION_ADMIN) {
            throw ApiException.forbidden("role-required-institution-admin",
                    "Only an institution admin can invite users.");
        }
        if (actor.getTenant().getType() != TenantType.INSTITUTION) {
            throw ApiException.conflict("invitation-not-supported",
                    "Invitations are only available for institutions.");
        }
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