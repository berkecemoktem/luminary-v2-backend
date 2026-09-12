package com.luminary.access.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Returns an RFC 9457 JSON 401 for {@code /api/**} and redirects browsers to
 * the OIDC authorization route for everything else.
 */
public class ApiAwareAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException {
        if (request.getRequestURI().startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(
                    "application/problem+json;charset=UTF-8");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String body = """
                    {"type":"about:blank","title":"Unauthorized",\
                    "status":401,"code":"authentication-required",\
                    "instance":"%s"}"""
                    .formatted(request.getRequestURI());
            response.getWriter().write(body);
        } else {
            response.sendRedirect("/oauth2/authorization/luminary");
        }
    }
}