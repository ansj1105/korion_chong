package com.korion.chong.settlement;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SettlementRequestRecord(
        long id,
        String requestNo,
        Long requesterUserId,
        String recipientType,
        Long recipientPartnerId,
        Long recipientMerchantId,
        Long walletAddressId,
        LocalDate periodStart,
        LocalDate periodEnd,
        String currency,
        BigDecimal requestedAmount,
        BigDecimal approvedAmount,
        BigDecimal heldAmount,
        String status
) {
}
