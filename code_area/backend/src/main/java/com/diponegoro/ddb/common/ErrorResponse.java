package com.diponegoro.ddb.common;

import java.time.Instant;
import java.util.Map;

/** Format error seragam untuk seluruh API. {@code fields} berisi pesan per field saat validasi gagal. */
public record ErrorResponse(String code, String message, Map<String, String> fields, Instant timestamp) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null, Instant.now());
    }
}
