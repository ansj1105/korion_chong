package com.korion.chong.auth;

import java.util.Optional;

public interface AuthRepository {
    boolean loginIdExists(String loginId);

    boolean applicationEmailExists(String email);

    boolean walletAddressExists(String walletAddress);

    Optional<ReferralCodeValidationResponse> findReferralCode(String code);

    long createSignupApplication(SignupApplicationRequest request);

    void recordActivity(String role, String actionType, String status, String targetType, Long targetId, String requestId);
}
