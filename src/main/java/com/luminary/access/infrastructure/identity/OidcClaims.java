package com.luminary.access.infrastructure.identity;

import com.luminary.access.domain.AuthIdentity;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Normalized view of the claims the application actually reads from the
 * identity provider. The provider adapter belongs to the access module.
 */
public record OidcClaims(AuthIdentity identity, String email,
                        String displayName, boolean emailVerified) {

    public static OidcClaims from(OidcUser oidcUser) {
        return new OidcClaims(
                new AuthIdentity(oidcUser.getIssuer().toString(),
                        oidcUser.getSubject()),
                oidcUser.getEmail(),
                displayNameOf(oidcUser),
                Boolean.TRUE.equals(oidcUser.getEmailVerified()));
    }

    private static String displayNameOf(OidcUser user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getGivenName() != null && !user.getGivenName().isBlank()) {
            return user.getGivenName();
        }
        String preferred = user.getPreferredUsername();
        return preferred == null ? "" : preferred;
    }
}