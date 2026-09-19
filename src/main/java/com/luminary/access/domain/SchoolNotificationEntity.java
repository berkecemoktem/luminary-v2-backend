package com.luminary.access.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A notification shown to an institution's admins when one of its
 * invitations is accepted or rejected by a student. Tenant-scoped; created
 * inside the same transaction that resolves the invitation.
 */
@Entity
@Table(name = "school_notifications")
public class SchoolNotificationEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private TenantEntity tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false)
    private SchoolNotificationType type;

    @Column(name = "student_user_id", updatable = false)
    private UUID studentUserId;

    @Column(name = "student_email", updatable = false)
    private String studentEmail;

    @Column(name = "student_display_name", updatable = false)
    private String studentDisplayName;

    @Column(name = "invitation_id", updatable = false)
    private UUID invitationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    protected SchoolNotificationEntity() {
    }

    private SchoolNotificationEntity(UUID id, TenantEntity tenant,
                                     SchoolNotificationType type,
                                     UUID studentUserId, String studentEmail,
                                     String studentDisplayName,
                                     UUID invitationId) {
        this.id = id;
        this.tenant = tenant;
        this.type = type;
        this.studentUserId = studentUserId;
        this.studentEmail = studentEmail;
        this.studentDisplayName = studentDisplayName;
        this.invitationId = invitationId;
        this.createdAt = OffsetDateTime.now();
    }

    public static SchoolNotificationEntity create(
            TenantEntity tenant, SchoolNotificationType type,
            UUID studentUserId, String studentEmail,
            String studentDisplayName, UUID invitationId) {
        return new SchoolNotificationEntity(UUID.randomUUID(), tenant, type,
                studentUserId, studentEmail, studentDisplayName, invitationId);
    }

    public UUID getId() {
        return id;
    }

    public TenantEntity getTenant() {
        return tenant;
    }

    public SchoolNotificationType getType() {
        return type;
    }

    public UUID getStudentUserId() {
        return studentUserId;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public String getStudentDisplayName() {
        return studentDisplayName;
    }

    public UUID getInvitationId() {
        return invitationId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getReadAt() {
        return readAt;
    }

    public boolean isRead() {
        return readAt != null;
    }

    public void markRead(OffsetDateTime now) {
        if (readAt == null) {
            this.readAt = now;
        }
    }
}