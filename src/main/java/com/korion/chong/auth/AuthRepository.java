package com.korion.chong.auth;

import java.util.Optional;

public interface AuthRepository {
    boolean loginIdExists(String loginId);

    boolean applicationEmailExists(String email);

    boolean telegramExists(String telegram);

    boolean whatsappExists(String whatsapp);

    boolean walletAddressExists(String walletAddress);

    Optional<ReferralCodeValidationResponse> findReferralCode(String code);

    void createEmailVerification(String email, String codeHash, java.time.Instant expiresAt, String requestId);

    boolean confirmEmailVerification(String email, String codeHash, java.time.Instant now);

    boolean emailVerified(String email);

    void recordWalletVerification(WalletLinkVerifyRequest request, String signatureHash, String status, String errorCode, String errorMessage);

    long createSignupApplication(SignupApplicationRequest request, String passwordHash);

    void recordActivity(String role, String actionType, String status, String targetType, Long targetId, String requestId);
}
