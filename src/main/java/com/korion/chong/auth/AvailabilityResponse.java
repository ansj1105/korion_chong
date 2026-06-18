package com.korion.chong.auth;

public record AvailabilityResponse(
        boolean available,
        String field,
        String resultCode,
        String messageKey
) {
    public static AvailabilityResponse available(String field) {
        return new AvailabilityResponse(true, field, "AVAILABLE", "auth.availability.available");
    }

    public static AvailabilityResponse duplicate(String field) {
        return new AvailabilityResponse(false, field, "DUPLICATE", "auth.availability.duplicate");
    }
}
