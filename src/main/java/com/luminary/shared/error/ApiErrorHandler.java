package com.luminary.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maps framework and application exceptions to the RFC 9457 Problem Details
 * contract. Response keeps {@code type, title, status, detail, instance,
 * code, traceId} plus optional per-field errors.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiErrorHandler {

    private static final URI PROBLEM_BASE = URI.create("https://luminary.dev/problems/");

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> handleApiException(ApiException ex, HttpServletRequest request) {
        org.springframework.http.ProblemDetail pd = build(ex.getStatus(), ex.getCode(),
                ex.getMessage(), ex.asProblemProperties());
        return applyInstance(pd, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex,
                                            HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.put(error.getField(), error.getDefaultMessage());
        }
        org.springframework.http.ProblemDetail pd = build(HttpStatus.UNPROCESSABLE_ENTITY,
                "validation-failed", "Request validation failed", fields);
        return applyInstance(pd, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Object> handleUnreadable(HttpMessageNotReadableException ex,
                                            HttpServletRequest request) {
        org.springframework.http.ProblemDetail pd = build(HttpStatus.BAD_REQUEST,
                "malformed-body", "Request body could not be read", Map.of());
        return applyInstance(pd, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex,
                                              HttpServletRequest request) {
        org.springframework.http.ProblemDetail pd = build(HttpStatus.FORBIDDEN,
                "access-denied", "Not authorized for this operation", Map.of());
        return applyInstance(pd, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Object> handleAuthentication(AuthenticationException ex,
                                                HttpServletRequest request) {
        org.springframework.http.ProblemDetail pd = build(HttpStatus.UNAUTHORIZED,
                "authentication-required", "Valid session required", Map.of());
        return applyInstance(pd, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Object> handleNoResource(NoResourceFoundException ex,
                                            HttpServletRequest request) {
        org.springframework.http.ProblemDetail pd = build(HttpStatus.NOT_FOUND,
                "resource-not-found", ex.getResourcePath(), Map.of());
        return applyInstance(pd, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Object> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        org.springframework.http.ProblemDetail pd = build(
                HttpStatus.METHOD_NOT_ALLOWED,
                "method-not-allowed", ex.getMessage(), Map.of());
        return applyInstance(pd, request);
    }

    @ExceptionHandler(Throwable.class)
    ResponseEntity<Object> handleUnexpected(Throwable ex, HttpServletRequest request) {
        org.slf4j.LoggerFactory.getLogger(ApiErrorHandler.class)
                .error("Unhandled error while processing request", ex);
        org.springframework.http.ProblemDetail pd = build(HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-error", "Unexpected internal error", Map.of());
        return applyInstance(pd, request);
    }

    private static org.springframework.http.ProblemDetail build(HttpStatus status, String code,
                                                                String detail,
                                                                Map<String, ?> extra) {
        org.springframework.http.ProblemDetail pd =
                org.springframework.http.ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        pd.setType(PROBLEM_BASE.resolve(code));
        pd.setProperty("code", code);
        pd.setProperty("traceId", traceId());
        if (!extra.isEmpty()) {
            pd.setProperty("fields", extra);
        }
        return pd;
    }

    private static ResponseEntity<Object> applyInstance(org.springframework.http.ProblemDetail pd,
                                                        HttpServletRequest request) {
        pd.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(pd.getStatus()).body((Object) pd);
    }

    private static String traceId() {
        String mdc = MDC.get("requestId");
        return mdc != null ? mdc : UUID.randomUUID().toString();
    }
}