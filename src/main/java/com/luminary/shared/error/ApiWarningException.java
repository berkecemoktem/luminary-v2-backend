package com.luminary.shared.error;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

/**
 * A recoverable business condition that still prevents the current operation
 * from completing. Successful responses with informational warnings should
 * carry those warnings in their response DTO instead of throwing this type.
 */
public final class ApiWarningException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, String> fields;

    private ApiWarningException(HttpStatus status, String code, String detail,
                                Map<String, String> fields) {
        super(detail);
        this.status = status;
        this.code = code;
        this.fields = fields.isEmpty()
                ? Collections.emptyMap()
                : Map.copyOf(fields);
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

    public static ApiWarningException badRequest(String code, String detail) {
        return new ApiWarningException(HttpStatus.BAD_REQUEST, code, detail,
                Map.of());
    }

    public static ApiWarningException conflict(String code, String detail) {
        return new ApiWarningException(HttpStatus.CONFLICT, code, detail,
                Map.of());
    }

    public static ApiWarningException validation(
            String code, String detail, Map<String, String> fields) {
        return new ApiWarningException(HttpStatus.UNPROCESSABLE_ENTITY,
                code, detail, fields);
    }
}
