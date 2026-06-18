package com.korion.chong.leader;

public record SignupApprovalResponse(
        long applicationId,
        String applicantType,
        String status,
        long userId,
        Long partnerId,
        Long merchantId,
        Long contractId,
        Long walletAddressId,
        String resultCode,
        String messageKey
) {
}
