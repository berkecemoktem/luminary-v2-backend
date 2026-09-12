package com.luminary.access.application.port;

import com.luminary.shared.identity.TenantId;
import com.luminary.shared.identity.UserId;

import java.util.Optional;

/**
 * Access to the authenticated server session (BFF model). The active tenant,
 * if any, is derived from this session and re-verified on every protected
 * call; it is never taken from the request body.
 */
public interface CurrentSession {

    Optional<UserId> userId();

    Optional<TenantId> activeTenant();

    void authenticate(UserId userId, TenantId activeTenant);

    void changeActiveTenant(TenantId tenantId);

    void clearActiveTenant();
}