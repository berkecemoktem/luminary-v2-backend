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
 * A single-use, time-limited invitation binding an email to a target role
 * within a tenant. Only the SHA-256 hash of the raw token is stored; the raw
 * token is handed to the inviter exactly once.
 */
@Entity
@Table(name = "invitations")
public class InvitationEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private TenantEntity tenant;

    @Column(name = "email", nullable = false, updatable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, updatable = false)
    private MembershipRole role;

    @Column(name = "token_hash", nullable = false, updatable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "accept_url", updatable = false)
    private String acceptUrl;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "created_by_user_id", updatable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected InvitationEntity() {
    }

    private InvitationEntity(UUID id, TenantEntity tenant, String email,
                             MembershipRole role, String tokenHash,
                             OffsetDateTime expiresAt, String acceptUrl,
                             UUID createdByUserId) {
        this.id = id;
        this.tenant = tenant;
        this.email = email;
        this.role = role;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.acceptUrl = acceptUrl;
        this.createdByUserId = createdByUserId;
        this.createdAt = OffsetDateTime.now();
    }

    public static InvitationEntity create(TenantEntity tenant, String email,
                                          MembershipRole role,
                                          String tokenHash,
                                          OffsetDateTime expiresAt,
                                          String acceptUrl,
                                          UUID createdByUserId) {
        return new InvitationEntity(UUID.randomUUID(), tenant, email, role,
                tokenHash, expiresAt, acceptUrl, createdByUserId);
    }

    public UUID getId() {
        return id;
    }

    public TenantEntity getTenant() {
        return tenant;
    }

    public String getEmail() {
        return email;
    }

    public MembershipRole getRole() {
        return role;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public String getAcceptUrl() {
        return acceptUrl;
    }

    public OffsetDateTime getUsedAt() {
        return usedAt;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(OffsetDateTime now) {
        return expiresAt.isBefore(now);
    }

    public void markUsed(OffsetDateTime now) {
        this.usedAt = now;
    }
}