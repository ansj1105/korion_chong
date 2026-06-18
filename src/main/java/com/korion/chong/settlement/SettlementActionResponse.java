package com.korion.chong.settlement;

import java.math.BigDecimal;

public record SettlementActionResponse(
        long settlementRequestId,
        String requestNo,
        String recipientType,
        Long recipientPartnerId,
        Long recipientMerchantId,
        BigDecimal requestedAmount,
        BigDecimal approvedAmount,
        BigDecimal heldAmount,
        String status,
        String resultCode,
        String messageKey
) {
}
