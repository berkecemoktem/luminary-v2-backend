package com.luminary.access.application;

import com.luminary.access.domain.MembershipEntity;
import com.luminary.access.domain.MembershipRole;
import com.luminary.access.domain.MembershipStatus;
import com.luminary.access.domain.SchoolNotificationEntity;
import com.luminary.access.domain.SchoolNotificationType;
import com.luminary.access.domain.TenantEntity;
import com.luminary.access.infrastructure.MembershipRepository;
import com.luminary.access.infrastructure.SchoolNotificationRepository;
import com.luminary.shared.error.ApiException;
import com.luminary.shared.identity.TenantId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * School-side notifications about invitation responses. Recording happens
 * from the {@link InvitationService} within the same transaction that
 * resolves the invitation; listing and mark-read are restricted to
 * institution admins of the active tenant.
 */
@Service
public class SchoolNotificationService {

    private final SchoolNotificationRepository notificationRepository;
    private final MembershipRepository membershipRepository;

    public SchoolNotificationService(
            SchoolNotificationRepository notificationRepository,
            MembershipRepository membershipRepository) {
        this.notificationRepository = notificationRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public void recordAccepted(TenantEntity tenant, UUID studentUserId,
                               String studentEmail, String studentDisplayName,
                               UUID invitationId) {
        record(tenant, SchoolNotificationType.INVITE_ACCEPTED,
                studentUserId, studentEmail, studentDisplayName, invitationId);
    }

    @Transactional
    public void recordRejected(TenantEntity tenant, UUID studentUserId,
                               String studentEmail, String studentDisplayName,
                               UUID invitationId) {
        record(tenant, SchoolNotificationType.INVITE_REJECTED,
                studentUserId, studentEmail, studentDisplayName, invitationId);
    }

    private void record(TenantEntity tenant, SchoolNotificationType type,
                        UUID studentUserId, String studentEmail,
                        String studentDisplayName, UUID invitationId) {
        notificationRepository.save(SchoolNotificationEntity.create(
                tenant, type, studentUserId, studentEmail,
                studentDisplayName, invitationId));
    }

    @Transactional(readOnly = true)
    public List<SchoolNotificationView> listForTenant(
            UUID actorUserId, TenantId tenantId) {
        requireInstitutionAdmin(actorUserId, tenantId);
        return notificationRepository
                .findByTenantIdOrderByCreatedAtDesc(tenantId.value())
                .stream()
                .map(SchoolNotificationView::from)
                .toList();
    }

    @Transactional
    public void markRead(UUID actorUserId, TenantId tenantId,
                         UUID notificationId) {
        requireInstitutionAdmin(actorUserId, tenantId);
        SchoolNotificationEntity notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> ApiException.notFound(
                        "notification-not-found",
                        "This notification does not exist."));
        if (!notification.getTenant().getId().equals(tenantId.value())) {
            throw ApiException.forbidden("notification-tenant-mismatch",
                    "This notification belongs to another workspace.");
        }
        notification.markRead(OffsetDateTime.now());
        notificationRepository.save(notification);
    }

    private void requireInstitutionAdmin(UUID actorUserId,
                                         TenantId tenantId) {
        MembershipEntity membership = membershipRepository
                .findByTenantIdAndUserId(tenantId.value(), actorUserId)
                .orElseThrow(() -> ApiException.notFound(
                        "membership-not-found",
                        "You are not a member of this workspace."));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw ApiException.forbidden("membership-revoked",
                    "Your membership in this workspace has been revoked.");
        }
        if (membership.getRole() != MembershipRole.INSTITUTION_ADMIN) {
            throw ApiException.forbidden("role-required-institution-admin",
                    "Only an institution admin can manage notifications.");
        }
    }
}