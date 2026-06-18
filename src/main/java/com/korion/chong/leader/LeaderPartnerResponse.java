package com.korion.chong.leader;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record LeaderPartnerResponse(
        List<PartnerSummary> items,
        PageMeta page
) {
    public record PartnerSummary(
            long partnerId,
            long userId,
            String loginId,
            String country,
            String region,
            String city,
            String status,
            long merchantCount,
            BigDecimal completedTransactionAmount,
            Instant lastActivityAt
    ) {
    }

    public record PageMeta(
            int page,
            int size,
            long totalItems
    ) {
    }
}
