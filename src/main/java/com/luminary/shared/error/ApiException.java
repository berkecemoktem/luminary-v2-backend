package com.luminary.shared.error;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

/**
 * Application error mapped to the RFC 9457 Problem Details contract
 * ({@code type, title, status, detail, instance, code, traceId, fields}).
 */
public final class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, String> fields;

    private ApiException(HttpStatus status, String code, String detail,
                         Map<String, String> fields) {
        super(detail);
        this.status = status;
        this.code = code;
        this.fields = fields.isEmpty() ? Collections.emptyMap() : Map.copyOf(fields);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public static ApiException of(HttpStatus status, String code,
                                  String detail) {
        return new ApiException(status, code, detail, Map.of());
    }

    public static ApiException unauthorized(String code, String detail) {
        return new ApiException(HttpStatus.UNAUTHORIZED, code, detail, Map.of());
    }

    public static ApiException forbidden(String code, String detail) {
        return new ApiException(HttpStatus.FORBIDDEN, code, detail, Map.of());
    }

    public static ApiException notFound(String code, String detail) {
        return new ApiException(HttpStatus.NOT_FOUND, code, detail, Map.of());
    }

    public static ApiException conflict(String code, String detail) {
        return new ApiException(HttpStatus.CONFLICT, code, detail, Map.of());
    }

    public static ApiException unprocessable(String code, String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code, detail, Map.of());
    }

    public static ApiException validation(String code, String detail,
                                          Map<String, String> fieldErrors) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code, detail, fieldErrors);
    }

    public static ApiException badRequest(String code, String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, detail, Map.of());
    }

    public static ApiException internal(String code, String detail) {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, code, detail, Map.of());
    }
}
