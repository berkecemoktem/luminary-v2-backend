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
 * The reliable authorization record: a user within a tenant with one role and
 * a runtime status. Role is never a global field on the user.
 */
@Entity
@Table(name = "memberships")
public class MembershipEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private TenantEntity tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, updatable = false)
    private MembershipRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MembershipStatus status;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    protected MembershipEntity() {
    }

    private MembershipEntity(UUID id, TenantEntity tenant, UserEntity user,
                             MembershipRole role) {
        this.id = id;
        this.tenant = tenant;
        this.user = user;
        this.role = role;
        this.status = MembershipStatus.ACTIVE;
        this.joinedAt = OffsetDateTime.now();
    }

    public static MembershipEntity create(TenantEntity tenant,
                                          UserEntity user,
                                          MembershipRole role) {
        return new MembershipEntity(UUID.randomUUID(), tenant, user, role);
    }

    public UUID getId() {
        return id;
    }

    public TenantEntity getTenant() {
        return tenant;
    }

    public UserEntity getUser() {
        return user;
    }

    public MembershipRole getRole() {
        return role;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public void revoke() {
        if (status == MembershipStatus.ACTIVE) {
            this.status = MembershipStatus.REVOKED;
            this.revokedAt = OffsetDateTime.now();
        }
    }

    public boolean isActive() {
        return status == MembershipStatus.ACTIVE;
    }
}