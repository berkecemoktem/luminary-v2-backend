package com.luminary.shared.error;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Creates the single RFC 9457 contract used by MVC and Spring Security.
 */
public final class ApiProblemFactory {

    private static final URI PROBLEM_BASE =
            URI.create("https://luminary.dev/problems/");

    private ApiProblemFactory() {
    }

    public static ProblemDetail create(
            HttpStatus status,
            String code,
            String detail,
            String requestUri,
            ApiProblemSeverity severity,
            Map<String, ?> fields) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(PROBLEM_BASE.resolve(code));
        problem.setInstance(URI.create(requestUri));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", code);
        problem.setProperty("severity", severity.name());
        problem.setProperty("traceId", traceId());
        if (fields != null && !fields.isEmpty()) {
            problem.setProperty("fields", fields);
        }
        return problem;
    }

    private static String traceId() {
        String requestId = MDC.get("requestId");
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }
}
