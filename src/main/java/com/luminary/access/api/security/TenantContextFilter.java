package com.luminary.access.api.security;

import com.luminary.access.application.port.CurrentSession;
import com.luminary.shared.identity.TenantId;
import com.luminary.shared.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Resolves the active tenant from the server session and places it into the
 * {@link TenantContext} ThreadLocal for the duration of the request.
 * Registered at an order just after the security filter chain
 * ({@code HIGHEST_PRECEDENCE + 10}) so authentication is already complete.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantContextFilter extends OncePerRequestFilter {

    private final CurrentSession currentSession;

    public TenantContextFilter(CurrentSession currentSession) {
        this.currentSession = currentSession;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        Optional<TenantId> tenant = currentSession.activeTenant();
        try {
            tenant.ifPresent(TenantContext::set);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}