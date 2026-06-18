package com.korion.chong.auth;

import java.time.Instant;

public record EmailVerificationSendResponse(
        String resultCode,
        String messageKey,
        Instant expiresAt
) {
}
