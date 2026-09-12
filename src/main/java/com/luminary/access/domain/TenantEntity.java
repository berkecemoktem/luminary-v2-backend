package com.luminary.access.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A working workspace: the data boundary of every tenant-scoped record. An
 * institution tenant has no row in the institution module until it is
 * provisioned there; a personal tenant has no institution record at all.
 */
@Entity
@Table(name = "tenants")
public class TenantEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false)
    private TenantType type;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TenantStatus status;

    @Column(name = "time_zone", nullable = false)
    private String timeZone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected TenantEntity() {
    }

    private TenantEntity(String id, TenantType type, String name,
                         String timeZone) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.status = TenantStatus.ACTIVE;
        this.timeZone = timeZone;
        this.createdAt = OffsetDateTime.now();
    }

    public static TenantEntity personal(String displayName) {
        return new TenantEntity(UUID.randomUUID().toString(),
                TenantType.PERSONAL, displayName, "UTC");
    }

    public static TenantEntity institution(String name, String timeZone) {
        return new TenantEntity(UUID.randomUUID().toString(),
                TenantType.INSTITUTION, name, timeZone);
    }

    public String getId() {
        return id;
    }

    public TenantType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public String getTimeZone() {
        return timeZone;
    }
}