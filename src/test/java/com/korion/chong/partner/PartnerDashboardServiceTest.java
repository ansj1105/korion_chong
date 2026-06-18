package com.korion.chong.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.korion.chong.leader.ForbiddenCountryScopeException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PartnerDashboardServiceTest {
    private final FakeRepository repository = new FakeRepository();
    private final PartnerDashboardService service = new PartnerDashboardService(
            repository,
            Clock.fixed(Instant.parse("2026-06-18T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void dashboardRejectsCountryOutsidePartnerScope() {
        repository.profile = new PartnerProfile(20L, 200L, "partner.kr", "SALES_PARTNER_APPROVED", "KR", "Seoul", "Seoul");

        assertThatThrownBy(() -> service.getDashboard(new PartnerAuthContext(20L, java.util.Set.of("US")), "US", "2026-06"))
                .isInstanceOf(ForbiddenCountryScopeException.class);
    }

    @Test
    void merchantSalesUsesMonthlyPeriodAndPartnerScope() {
        repository.profile = new PartnerProfile(20L, 200L, "partner.kr", "SALES_PARTNER_APPROVED", "KR", "Seoul", "Seoul");

        Map<String, Object> response = service.getMerchantSales(new PartnerAuthContext(20L, java.util.Set.of("KR")), "KR", "2026-06");

        assertThat(response).containsEntry("t1Rows", List.of());
        assertThat(repository.lastPartnerId).isEqualTo(20L);
        assertThat(repository.lastCountryScope).isEqualTo("KR");
        assertThat(repository.lastPeriodStart).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(repository.lastPeriodEnd).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    private static class FakeRepository implements PartnerDashboardRepository {
        PartnerProfile profile;
        long lastPartnerId;
        String lastCountryScope;
        LocalDate lastPeriodStart;
        LocalDate lastPeriodEnd;

        @Override
        public Optional<PartnerProfile> findPartnerProfile(long partnerId) {
            return Optional.ofNullable(profile);
        }

        @Override
        public Map<String, Object> findDashboard(long partnerId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
            return Map.of("kpis", List.of());
        }

        @Override
        public Map<String, Object> findMerchantApplications(long partnerId, String countryScope) {
            return Map.of("stats", List.of(), "rows", List.of());
        }

        @Override
        public Map<String, Object> findMerchantApplicationDetail(long partnerId, String countryScope) {
            return Map.of("code", "SP-00020");
        }

        @Override
        public Map<String, Object> findMerchants(long partnerId, String countryScope) {
            return Map.of("stats", List.of(), "rows", List.of());
        }

        @Override
        public Map<String, Object> findMerchantSales(long partnerId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
            lastPartnerId = partnerId;
            lastCountryScope = countryScope;
            lastPeriodStart = periodStart;
            lastPeriodEnd = periodEnd;
            return Map.of("stats", List.of(), "t1Rows", List.of(), "t2Rows", List.of());
        }

        @Override
        public Map<String, Object> findSettlementRequestSummary(long partnerId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
            return Map.of("stats", List.of());
        }

        @Override
        public Map<String, Object> findSettlementHistory(long partnerId, String countryScope) {
            return Map.of("rows", List.of());
        }

        @Override
        public Map<String, Object> findSettlementDetail(long partnerId, String countryScope) {
            return Map.of("basicInfo", List.of());
        }

        @Override
        public Map<String, Object> findHqNotices(long partnerId, String countryScope) {
            return Map.of("rows", List.of());
        }

        @Override
        public Map<String, Object> findNoticeSendSummary(long partnerId, String countryScope) {
            return Map.of("metrics", List.of());
        }

        @Override
        public Map<String, Object> findNoticeHistory(long partnerId, String countryScope) {
            return Map.of("metrics", List.of(), "rows", List.of());
        }

        @Override
        public Map<String, Object> findProfile(long partnerId, String countryScope) {
            return Map.of("code", "SP-00020");
        }

        @Override
        public Map<String, Object> findActivityLogs(long partnerId, String countryScope) {
            return Map.of("metrics", List.of(), "rows", List.of());
        }
    }
}
