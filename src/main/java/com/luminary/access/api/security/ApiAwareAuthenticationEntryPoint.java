package com.luminary.access.api.security;

import com.luminary.shared.error.ApiProblemWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Returns an RFC 9457 JSON 401 for {@code /api/**} and redirects browsers to
 * the OIDC authorization route for everything else.
 */
public class ApiAwareAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final ApiProblemWriter problemWriter;

    public ApiAwareAuthenticationEntryPoint(ApiProblemWriter problemWriter) {
        this.problemWriter = problemWriter;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException {
        if (request.getRequestURI().startsWith("/api/")) {
            problemWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                    "authentication-required",
                    "A valid session is required.");
        } else {
            response.sendRedirect("/oauth2/authorization/luminary");
        }
    }
}
