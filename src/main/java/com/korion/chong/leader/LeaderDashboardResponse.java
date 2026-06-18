package com.korion.chong.leader;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record LeaderDashboardResponse(
        LeaderProfile leaderProfile,
        Kpis kpis,
        OrganizationSummary organizationSummary,
        List<MonthlyVolume> monthlyVolume,
        FeeSummary feeSummary,
        List<RiskAlert> riskAlerts
) {
    public record Kpis(
            long approvedPartnerCount,
            long approvedMerchantCount,
            BigDecimal completedTransactionAmount,
            BigDecimal confirmedCommissionAmount
    ) {
    }

    public record OrganizationSummary(
            long partnerCount,
            long merchantCount
    ) {
    }

    public record MonthlyVolume(
            YearMonth month,
            BigDecimal amount,
            long transactionCount
    ) {
    }

    public record FeeSummary(
            BigDecimal countryLeaderFee,
            BigDecimal salesPartnerFee,
            BigDecimal korionFee
    ) {
    }

    public record RiskAlert(
            String type,
            String severity,
            String message
    ) {
    }
}
