package com.korion.chong.auth;

public record TelegramVerificationConfirmResponse(
        boolean verified,
        String resultCode,
        String messageKey
) {
}
