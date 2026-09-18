package com.luminary.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps framework and application exceptions to the RFC 9457 Problem Details
 * contract. Response keeps {@code type, title, status, detail, instance,
 * code, traceId} plus optional per-field errors.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiErrorHandler {

    private static final Logger log =
            LoggerFactory.getLogger(ApiErrorHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> handleApiException(ApiException ex,
                                              HttpServletRequest request) {
        return respond(ex.getStatus(), ex.getCode(), ex.getMessage(),
                ex.getFields(), ApiProblemSeverity.ERROR, request);
    }

    @ExceptionHandler(ApiWarningException.class)
    ResponseEntity<Object> handleApiWarning(ApiWarningException ex,
                                            HttpServletRequest request) {
        log.warn("Business warning [{}]: {}", ex.getCode(), ex.getMessage());
        return respond(ex.getStatus(), ex.getCode(), ex.getMessage(),
                ex.getFields(), ApiProblemSeverity.WARNING, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex,
                                            HttpServletRequest request) {
        return validation(ex.getBindingResult(), request);
    }

    @ExceptionHandler(BindException.class)
    ResponseEntity<Object> handleBinding(BindException ex,
                                         HttpServletRequest request) {
        return validation(ex.getBindingResult(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Object> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        Map<String, String> fields = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (first, ignored) -> first));
        return respond(HttpStatus.UNPROCESSABLE_ENTITY,
                "validation-failed", "Request validation failed.", fields,
                ApiProblemSeverity.ERROR, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Object> handleUnreadable(HttpMessageNotReadableException ex,
                                            HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "malformed-body",
                "Request body could not be read.", Map.of(),
                ApiProblemSeverity.ERROR, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Object> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        String expected = ex.getRequiredType() == null
                ? "the expected type"
                : ex.getRequiredType().getSimpleName();
        return respond(HttpStatus.BAD_REQUEST, "invalid-parameter",
                "Parameter '" + ex.getName() + "' must be " + expected + ".",
                Map.of(), ApiProblemSeverity.ERROR, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<Object> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "missing-parameter",
                "Required parameter '" + ex.getParameterName()
                        + "' is missing.",
                Map.of(), ApiProblemSeverity.ERROR, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Object> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        String detail = ex.getMessage() == null || ex.getMessage().isBlank()
                ? "The request contains an invalid argument."
                : ex.getMessage();
        return respond(HttpStatus.BAD_REQUEST, "invalid-argument", detail,
                Map.of(), ApiProblemSeverity.ERROR, request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<Object> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, "invalid-credentials",
                "Invalid credentials.", Map.of(), ApiProblemSeverity.ERROR,
                request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex,
                                              HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, "access-denied",
                "Not authorized for this operation.", Map.of(),
                ApiProblemSeverity.ERROR, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Object> handleAuthentication(AuthenticationException ex,
                                                HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, "authentication-required",
                "A valid session is required.", Map.of(),
                ApiProblemSeverity.ERROR, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Object> handleNoResource(NoResourceFoundException ex,
                                            HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "resource-not-found",
                "The requested resource was not found.", Map.of(),
                ApiProblemSeverity.ERROR, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Object> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "method-not-allowed",
                "The HTTP method is not supported for this endpoint.",
                Map.of(), ApiProblemSeverity.ERROR, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<Object> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "media-type-not-supported",
                "The request media type is not supported.", Map.of(),
                ApiProblemSeverity.ERROR, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        log.warn("Data integrity violation while processing {} {}",
                request.getMethod(), request.getRequestURI());
        return respond(HttpStatus.CONFLICT, "data-conflict",
                "The operation conflicts with existing data.", Map.of(),
                ApiProblemSeverity.ERROR, request);
    }

    @ExceptionHandler(Throwable.class)
    ResponseEntity<Object> handleUnexpected(Throwable ex,
                                            HttpServletRequest request) {
        log.error("Unhandled error while processing {} {}",
                request.getMethod(), request.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "An unexpected internal error occurred.", Map.of(),
                ApiProblemSeverity.ERROR, request);
    }

    private static ResponseEntity<Object> validation(
            BindingResult bindingResult, HttpServletRequest request) {
        Map<String, String> fields = bindingResult.getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        error -> String.valueOf(error.getDefaultMessage()),
                        (first, ignored) -> first));
        return respond(HttpStatus.UNPROCESSABLE_ENTITY,
                "validation-failed", "Request validation failed.", fields,
                ApiProblemSeverity.ERROR, request);
    }

    private static ResponseEntity<Object> respond(
            HttpStatus status,
            String code,
            String detail,
            Map<String, ?> fields,
            ApiProblemSeverity severity,
            HttpServletRequest request) {
        return ResponseEntity.status(status).body(
                ApiProblemFactory.create(status, code, detail,
                        request.getRequestURI(), severity, fields));
    }
}
