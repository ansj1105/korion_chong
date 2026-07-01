package com.korion.chong.auth;

public record WalletAddressValidateResponse(
        boolean verified,
        String walletNetwork,
        String authStatus,
        String resultCode,
        String messageKey
) {
}
