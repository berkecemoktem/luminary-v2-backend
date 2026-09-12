package com.luminary.access.api;

import com.luminary.access.application.port.CurrentSession;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lightweight access log: one INFO line per request with method, path,
 * status, duration and the authenticated user when a session is present.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RequestLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    private final CurrentSession currentSession;

    public RequestLogFilter(CurrentSession currentSession) {
        this.currentSession = currentSession;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        long started = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long millis = (System.nanoTime() - started) / 1_000_000;
            String user = currentSession.userId()
                    .map(id -> " user=" + id.value())
                    .orElse(" user=anonymous");
            log.info("{} {} -> {}{} ({}ms)",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), user, millis);
        }
    }
}