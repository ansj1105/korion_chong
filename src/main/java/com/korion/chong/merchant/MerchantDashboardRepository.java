package com.korion.chong.merchant;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

public interface MerchantDashboardRepository {
    Optional<MerchantProfile> findMerchantProfile(long merchantId);

    Map<String, Object> findDashboard(long merchantId, String countryScope, LocalDate periodStart, LocalDate periodEnd);

    Map<String, Object> findTransactions(long merchantId, String countryScope, LocalDate periodStart, LocalDate periodEnd, String variant);

    Map<String, Object> findStore(long merchantId, String countryScope);

    Map<String, Object> findProfile(long merchantId, String countryScope);

    Map<String, Object> findHqNotices(long merchantId, String countryScope);

    Map<String, Object> findActivityLogs(long merchantId, String countryScope);

    Map<String, Object> findSettlementHistory(long merchantId, String countryScope);

    Map<String, Object> findSettlementDetail(long merchantId, String countryScope);
}
