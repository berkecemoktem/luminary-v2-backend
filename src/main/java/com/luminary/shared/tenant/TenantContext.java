package com.luminary.shared.tenant;

import com.luminary.shared.identity.TenantId;

import java.util.Optional;

/**
 * Request-scoped working tenant derived from the authenticated server session
 * and its active membership. Never trusted from the request body; the tenant
 * selection endpoint is only a candidate that the server re-verifies.
 */
public final class TenantContext {

    private static final ThreadLocal<TenantId> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(TenantId tenantId) {
        CURRENT.set(tenantId);
    }

    public static Optional<TenantId> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static TenantId require() {
        return get().orElseThrow(() -> new IllegalStateException(
                "Tenant context is not set for the current request"));
    }

    public static void clear() {
        CURRENT.remove();
    }
}