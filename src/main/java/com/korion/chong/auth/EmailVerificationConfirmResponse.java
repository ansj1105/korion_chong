package com.korion.chong.auth;

public record EmailVerificationConfirmResponse(
        boolean verified,
        String resultCode,
        String messageKey
) {
}
