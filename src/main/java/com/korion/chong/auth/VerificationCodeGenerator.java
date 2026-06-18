package com.korion.chong.auth;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class VerificationCodeGenerator {
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSixDigitCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
