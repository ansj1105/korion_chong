package com.korion.chong.merchant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.korion.chong.leader.ForbiddenCountryScopeException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MerchantDashboardServiceTest {
    private final FakeRepository repository = new FakeRepository();
    private final MerchantDashboardService service = new MerchantDashboardService(
            repository,
            Clock.fixed(Instant.parse("2026-06-19T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void dashboardRejectsCountryOutsideMerchantScope() {
        repository.profile = new MerchantProfile(30L, 300L, "merchant.kr", "Kori Cafe", "MERCHANT_APPROVED", "KR", "Seoul", "Seoul");

        assertThatThrownBy(() -> service.getDashboard(new MerchantAuthContext(30L, java.util.Set.of("US")), "US", "2026-06"))
                .isInstanceOf(ForbiddenCountryScopeException.class);
    }

    private static class FakeRepository implements MerchantDashboardRepository {
        MerchantProfile profile;

        @Override
        public Optional<MerchantProfile> findMerchantProfile(long merchantId) {
            return Optional.ofNullable(profile);
        }

        @Override
        public Map<String, Object> findDashboard(long merchantId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findTransactions(long merchantId, String countryScope, LocalDate periodStart, LocalDate periodEnd, String variant) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findStore(long merchantId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findProfile(long merchantId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findHqNotices(long merchantId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findActivityLogs(long merchantId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findSettlementHistory(long merchantId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findSettlementDetail(long merchantId, String countryScope) {
            return Map.of();
        }
    }
}
