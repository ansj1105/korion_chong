package com.korion.chong.auth;

import java.time.Instant;
import java.util.List;

public record LoginResponse(
        boolean authenticated,
        long userId,
        String role,
        Long partnerId,
        Long merchantId,
        List<String> countryScopes,
        String redirectPath,
        boolean requiresTwoFactor,
        Instant sessionExpiresAt,
        String resultCode,
        String messageKey
) {
}
