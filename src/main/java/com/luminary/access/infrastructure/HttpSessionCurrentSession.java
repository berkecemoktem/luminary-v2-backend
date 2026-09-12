package com.luminary.access.infrastructure;

import com.luminary.access.application.port.CurrentSession;
import com.luminary.shared.identity.TenantId;
import com.luminary.shared.identity.UserId;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

/**
 * Stores the BFF marker attributes in the Spring Session-backed
 * {@link HttpSession}. Values are stored as UUID strings because Spring
 * Session JDBC serializes attributes to a byte column.
 */
@Component
public class HttpSessionCurrentSession implements CurrentSession {

    private static final String ATTR_USER_ID = "luminary.ctx.userId";
    private static final String ATTR_ACTIVE_TENANT = "luminary.ctx.activeTenant";

    @Override
    public Optional<UserId> userId() {
        return stringAttr(ATTR_USER_ID).map(UserId::fromString);
    }

    @Override
    public Optional<TenantId> activeTenant() {
        return stringAttr(ATTR_ACTIVE_TENANT).map(TenantId::fromString);
    }

    @Override
    public void authenticate(UserId userId, TenantId activeTenant) {
        HttpSession session = currentSession();
        session.setAttribute(ATTR_USER_ID, userId.value().toString());
        if (activeTenant == null) {
            session.removeAttribute(ATTR_ACTIVE_TENANT);
        } else {
            session.setAttribute(ATTR_ACTIVE_TENANT, activeTenant.value().toString());
        }
    }

    @Override
    public void changeActiveTenant(TenantId tenantId) {
        currentSession().setAttribute(ATTR_ACTIVE_TENANT, tenantId.value().toString());
    }

    @Override
    public void clearActiveTenant() {
        currentSession().removeAttribute(ATTR_ACTIVE_TENANT);
    }

    private static Optional<String> stringAttr(String name) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(attrs.getRequest().getSession(false))
                .map(session -> session.getAttribute(name))
                .filter(value -> value instanceof UUID || value instanceof String)
                .map(Object::toString);
    }

    private static HttpSession currentSession() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new IllegalStateException("No active HTTP request");
        }
        return attrs.getRequest().getSession(true);
    }
}