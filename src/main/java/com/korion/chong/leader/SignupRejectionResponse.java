package com.korion.chong.leader;

public record SignupRejectionResponse(
        long applicationId,
        String applicantType,
        String status,
        String resultCode,
        String messageKey
) {
}
