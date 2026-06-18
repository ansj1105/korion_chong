package com.korion.chong.leader;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LeaderDashboardRepository {
    Optional<LeaderProfile> findLeaderProfile(long leaderId);

    LeaderDashboardResponse.Kpis findKpis(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd);

    List<LeaderDashboardResponse.MonthlyVolume> findMonthlyVolume(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd);

    LeaderDashboardResponse.FeeSummary findFeeSummary(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd);

    List<LeaderPartnerResponse.PartnerSummary> findPartners(
            long leaderId,
            String countryScope,
            PartnerSearchCriteria criteria
    );

    long countPartners(long leaderId, String countryScope, PartnerSearchCriteria criteria);

    Map<String, Object> findSignupApplications(long leaderId, String countryScope, String applicantType);

    Map<String, Object> findPartnerSales(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd);

    Map<String, Object> findMerchants(long leaderId, String countryScope);

    Map<String, Object> findMerchantSales(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd);

    Map<String, Object> findTransactions(long leaderId, String countryScope, String variant, LocalDate periodStart, LocalDate periodEnd);

    Map<String, Object> findSettlementRequestSummary(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd);

    Map<String, Object> findSettlementHistory(long leaderId, String countryScope);

    Map<String, Object> findSettlementDetail(long leaderId, String countryScope);

    Map<String, Object> findHqNotices(long leaderId, String countryScope);

    Map<String, Object> findNoticeSendSummary(long leaderId, String countryScope);

    Map<String, Object> findNoticeHistory(long leaderId, String countryScope);

    Map<String, Object> findProfile(long leaderId, String countryScope);

    Map<String, Object> findActivityLogs(long leaderId, String countryScope);
}
