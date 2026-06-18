package com.korion.chong.leader;

import java.time.Instant;
import java.util.Optional;

public interface SignupApprovalRepository {
    Optional<SignupApplicationApprovalRecord> findApplicationForUpdate(long applicationId);

    long createUser(SignupApplicationApprovalRecord application);

    long createPartner(SignupApplicationApprovalRecord application, long userId, long approvedByUserId, Long parentPartnerId, Instant now);

    long createMerchant(SignupApplicationApprovalRecord application, long userId, long approvedByUserId, Long leaderPartnerId, Long salesPartnerId, Instant now);

    long createContract(SignupApplicationApprovalRecord application, long approvedByUserId, Long leaderPartnerId, Long salesPartnerId, Long merchantId, Instant now);

    Long createWalletAddress(SignupApplicationApprovalRecord application, long userId, Long partnerId, Long merchantId, Instant now);

    void approveApplication(long applicationId, Instant now);

    void rejectApplication(long applicationId, String reason, Instant now);

    long findUserIdByLeaderId(long leaderId);

    void recordActivity(String role, String actionType, String status, String targetType, Long targetId, String requestId);
}
