package com.luminary.access.api;

import com.luminary.access.application.port.CurrentSession;
import com.luminary.shared.error.ApiException;
import com.luminary.shared.identity.TenantId;
import com.luminary.shared.identity.UserId;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated identity from the BFF server session. Used by
 * every protected endpoint; the security layer guarantees an authenticated
 * session before {@link CurrentUser} is consulted.
 */
@Component
public class CurrentUser {

    private final CurrentSession currentSession;

    public CurrentUser(CurrentSession currentSession) {
        this.currentSession = currentSession;
    }

    public UserId require() {
        return currentSession.userId().orElseThrow(() ->
                ApiException.unauthorized("unauthenticated",
                        "Authentication required."));
    }

    public TenantId requireTenant() {
        return currentSession.activeTenant().orElseThrow(() ->
                ApiException.unauthorized("no-active-workspace",
                        "No active workspace in session."));
    }
}