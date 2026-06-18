package com.korion.chong.auth;

public record UserCredential(
        long userId,
        String loginId,
        String passwordHash,
        String status
) {
}
