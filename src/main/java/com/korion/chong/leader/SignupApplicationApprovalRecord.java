package com.korion.chong.leader;

public record SignupApplicationApprovalRecord(
        long applicationId,
        String applicantType,
        String loginId,
        String passwordHash,
        String email,
        String companyName,
        String contactName,
        String phone,
        String referralCode,
        Long ownerPartnerId,
        String ownerPartnerType,
        Long ownerLeaderPartnerId,
        String country,
        String region,
        String city,
        String address,
        String businessType,
        String walletNetwork,
        String walletAddress,
        String walletAuthStatus,
        String contractPath,
        String status
) {
}
