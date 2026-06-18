package com.korion.chong.auth;

public record WalletLinkVerifyResponse(
        boolean verified,
        String authStatus,
        String resultCode,
        String messageKey
) {
}
