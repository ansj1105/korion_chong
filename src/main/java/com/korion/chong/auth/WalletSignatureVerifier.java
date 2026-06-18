package com.korion.chong.auth;

public interface WalletSignatureVerifier {
    boolean verifyTronSignature(String address, String nonce, String signature);
}
