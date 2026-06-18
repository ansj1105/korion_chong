package com.korion.chong.auth;

import org.springframework.stereotype.Component;

@Component
public class UnavailableWalletSignatureVerifier implements WalletSignatureVerifier {
    @Override
    public boolean verifyTronSignature(String address, String nonce, String signature) {
        return false;
    }
}
