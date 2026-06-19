package com.korion.chong.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TelegramVerificationSendRequest(
        @NotBlank @Size(max = 120) String telegram,
        @Size(max = 128) String requestId
) {
}
