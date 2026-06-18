package com.korion.chong.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WalletLinkVerifyRequest(
        @NotBlank @Pattern(regexp = "PARTNER|MERCHANT") String applicantType,
        @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 255) String walletAddress,
        @NotBlank @Size(max = 128) String nonce,
        @NotBlank @Size(max = 4096) String signature,
        @Size(max = 128) String requestId
) {
}
