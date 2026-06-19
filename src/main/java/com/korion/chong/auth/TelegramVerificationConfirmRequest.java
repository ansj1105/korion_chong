package com.korion.chong.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TelegramVerificationConfirmRequest(
        @NotBlank @Size(max = 120) String telegram,
        @NotBlank @Pattern(regexp = "\\d{6}") String code,
        @Size(max = 128) String requestId
) {
}
