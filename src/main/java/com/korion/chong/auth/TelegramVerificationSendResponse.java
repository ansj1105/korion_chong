package com.korion.chong.auth;

import java.time.Instant;

public record TelegramVerificationSendResponse(
        String resultCode,
        String messageKey,
        Instant expiresAt
) {
}
