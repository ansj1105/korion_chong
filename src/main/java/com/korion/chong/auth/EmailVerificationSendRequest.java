package com.korion.chong.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailVerificationSendRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 128) String requestId
) {
}
