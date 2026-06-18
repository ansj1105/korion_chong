package com.korion.chong.partner;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

public interface PartnerDashboardRepository {
    Optional<PartnerProfile> findPartnerProfile(long partnerId);

    Map<String, Object> findDashboard(long partnerId, String countryScope, LocalDate periodStart, LocalDate periodEnd);

    Map<String, Object> findMerchantApplications(long partnerId, String countryScope);

    Map<String, Object> findMerchantApplicationDetail(long partnerId, String countryScope);

    Map<String, Object> findMerchants(long partnerId, String countryScope);

    Map<String, Object> findMerchantSales(long partnerId, String countryScope, LocalDate periodStart, LocalDate periodEnd);

    Map<String, Object> findSettlementRequestSummary(long partnerId, String countryScope, LocalDate periodStart, LocalDate periodEnd);

    Map<String, Object> findSettlementHistory(long partnerId, String countryScope);

    Map<String, Object> findSettlementDetail(long partnerId, String countryScope);

    Map<String, Object> findHqNotices(long partnerId, String countryScope);

    Map<String, Object> findNoticeSendSummary(long partnerId, String countryScope);

    Map<String, Object> findNoticeHistory(long partnerId, String countryScope);

    Map<String, Object> findProfile(long partnerId, String countryScope);

    Map<String, Object> findActivityLogs(long partnerId, String countryScope);
}
