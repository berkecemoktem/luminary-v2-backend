package com.luminary.access.domain;

/**
 * An OIDC provider identity pair. A single email from two different issuers
 * is treated as a separate user and must never be merged.
 */
public record AuthIdentity(String issuer, String subject) {

    public AuthIdentity {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("issuer must not be blank");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
    }
}