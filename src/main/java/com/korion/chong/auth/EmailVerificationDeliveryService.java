package com.korion.chong.auth;

import java.time.Instant;

public interface EmailVerificationDeliveryService {
    void sendVerificationCode(String email, String code, Instant expiresAt);
}
