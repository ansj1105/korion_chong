package com.korion.chong.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupApplicationRequest(
        @NotBlank @Pattern(regexp = "PARTNER|MERCHANT") String applicantType,
        @NotBlank @Size(max = 50) String loginId,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 160) String companyName,
        @NotBlank @Size(max = 120) String contactName,
        @Size(max = 80) String phone,
        @Size(max = 120) String telegram,
        @Size(max = 80) String whatsapp,
        @Size(max = 64) String referralCode,
        @Pattern(regexp = "[A-Z]{2}") String country,
        @Size(max = 100) String region,
        @Size(max = 100) String city,
        @Size(max = 1000) String address,
        @Size(max = 80) String businessType,
        @Size(max = 255) String walletAddress,
        @Size(max = 4000) String integrationPlan,
        @Size(max = 4000) String evidenceNote,
        @Size(max = 128) String requestId
) {
}
