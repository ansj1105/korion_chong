package com.korion.chong.settlement;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public interface SettlementRepository {
    BigDecimal availablePartnerAmount(long partnerId, LocalDate periodStart, LocalDate periodEnd);

    long createPartnerRequest(
            long requesterUserId,
            String recipientType,
            long recipientPartnerId,
            Long walletAddressId,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal requestedAmount,
            String memo,
            Instant now
    );

    void attachPartnerCommissions(long settlementRequestId, long partnerId, LocalDate periodStart, LocalDate periodEnd);

    Optional<SettlementRequestRecord> findForUpdate(long settlementRequestId);

    boolean canLeaderManage(long leaderPartnerId, SettlementRequestRecord request);

    SettlementRequestRecord approve(long settlementRequestId, long reviewedByUserId, BigDecimal approvedAmount, BigDecimal heldAmount, String note, Instant now);

    SettlementRequestRecord reject(long settlementRequestId, long reviewedByUserId, String note, Instant now);

    SettlementRequestRecord markPaid(long settlementRequestId, long reviewedByUserId, Instant now);

    void recordActivity(String role, String actionType, String status, String targetType, Long targetId, String requestId);
}
