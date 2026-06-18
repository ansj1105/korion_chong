package com.korion.chong.auth;

public record SignupApplicationResponse(
        long applicationId,
        String status,
        String resultCode,
        String messageKey,
        boolean walletStored
) {
}
