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
 * Global user identity mapped from an OIDC {@code issuer + subject}. Email is
 * a derived attribute, never an identity key.
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "auth_issuer", nullable = false, updatable = false)
    private String authIssuer;

    @Column(name = "auth_subject", nullable = false, updatable = false)
    private String authSubject;

    @Column(name = "email")
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "country")
    private String country;

    @Column(name = "city")
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected UserEntity() {
    }

    private UserEntity(UUID id, AuthIdentity identity, String email,
                       String displayName, AccountStatus status) {
        this.id = id;
        this.authIssuer = identity.issuer();
        this.authSubject = identity.subject();
        this.email = email;
        this.displayName = displayName;
        this.status = status;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public static UserEntity create(AuthIdentity identity, String email,
                                    String displayName) {
        return new UserEntity(UUID.randomUUID(), identity, email, displayName,
                AccountStatus.ACTIVE);
    }

    public UUID getId() {
        return id;
    }

    public AuthIdentity getIdentity() {
        return new AuthIdentity(authIssuer, authSubject);
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void markUpdated() {
        this.updatedAt = OffsetDateTime.now();
    }
}