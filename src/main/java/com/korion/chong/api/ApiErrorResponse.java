package com.korion.chong.api;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        String code,
        String message
) {
    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(Instant.now(), code, message);
    }
}
