package com.luminary.access.application;

import com.luminary.access.domain.SchoolNotificationEntity;
import com.luminary.access.domain.SchoolNotificationType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read view of a school notification displayed in the admin bell.
 */
public record SchoolNotificationView(UUID notificationId,
                                     SchoolNotificationType type,
                                     String tenantId, UUID studentUserId,
                                     String studentEmail,
                                     String studentDisplayName,
                                     UUID invitationId,
                                     OffsetDateTime createdAt,
                                     OffsetDateTime readAt) {

    public static SchoolNotificationView from(
            SchoolNotificationEntity entity) {
        return new SchoolNotificationView(
                entity.getId(), entity.getType(),
                entity.getTenant().getId(), entity.getStudentUserId(),
                entity.getStudentEmail(), entity.getStudentDisplayName(),
                entity.getInvitationId(), entity.getCreatedAt(),
                entity.getReadAt());
    }
}