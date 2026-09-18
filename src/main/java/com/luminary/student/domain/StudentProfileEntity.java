package com.luminary.student.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tenant-scoped student profile. Holds academic attributes that are
 * meaningful within a specific institution workspace: grade level,
 * academic field, and school name. One profile per user per tenant.
 */
@Entity
@Table(name = "student_profiles")
public class StudentProfileEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_level", nullable = false)
    private GradeLevel gradeLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "field", nullable = false)
    private StudentField field;

    @Column(name = "school_name")
    private String schoolName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected StudentProfileEntity() {
    }

    private StudentProfileEntity(UUID id, String tenantId, UUID userId,
                                 GradeLevel gradeLevel, StudentField field,
                                 String schoolName) {
        this.id = id;
        this.tenantId = tenantId;
        this.userId = userId;
        this.gradeLevel = gradeLevel;
        this.field = field;
        this.schoolName = schoolName;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public static StudentProfileEntity create(String tenantId, UUID userId,
                                              GradeLevel gradeLevel,
                                              StudentField field,
                                              String schoolName) {
        return new StudentProfileEntity(UUID.randomUUID(), tenantId, userId,
                gradeLevel, field, schoolName);
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public GradeLevel getGradeLevel() {
        return gradeLevel;
    }

    public StudentField getField() {
        return field;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setGradeLevel(GradeLevel gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public void setField(StudentField field) {
        this.field = field;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public void markUpdated() {
        this.updatedAt = OffsetDateTime.now();
    }
}
