package com.korion.chong.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WalletAddressValidateRequest(
        @NotBlank @Pattern(regexp = "PARTNER|MERCHANT") String applicantType,
        @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 255) String walletAddress,
        @Size(max = 128) String requestId
) {
}
