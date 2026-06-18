package com.korion.chong.leader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LeaderDashboardServiceTest {
    private final FakeRepository repository = new FakeRepository();
    private final LeaderDashboardService service = new LeaderDashboardService(
            repository,
            Clock.fixed(Instant.parse("2026-06-18T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void dashboardRejectsCountryOutsideLeaderScope() {
        repository.profile = new LeaderProfile(10L, 100L, "leader.kr", "COUNTRY_LEADER_APPROVED", List.of("KR"));

        assertThatThrownBy(() -> service.getDashboard(new AuthContext(10L, java.util.Set.of("US")), "2026-06", "US"))
                .isInstanceOf(ForbiddenCountryScopeException.class);
    }

    @Test
    void dashboardUsesMonthlyPeriodAndReturnsSummary() {
        repository.profile = new LeaderProfile(10L, 100L, "leader.kr", "COUNTRY_LEADER_APPROVED", List.of("KR"));
        repository.kpis = new LeaderDashboardResponse.Kpis(2, 3, new BigDecimal("100.00"), new BigDecimal("25.00"));

        LeaderDashboardResponse response = service.getDashboard(new AuthContext(10L, java.util.Set.of("KR")), "2026-06", "KR");

        assertThat(response.leaderProfile().leaderId()).isEqualTo(10L);
        assertThat(response.kpis().approvedPartnerCount()).isEqualTo(2);
        assertThat(repository.lastPeriodStart).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(repository.lastPeriodEnd).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @Test
    void partnersRequireCountryScopeAndReturnRepositoryPage() {
        repository.profile = new LeaderProfile(10L, 100L, "leader.kr", "COUNTRY_LEADER_APPROVED", List.of("KR"));
        repository.partners.add(new LeaderPartnerResponse.PartnerSummary(
                20L,
                200L,
                "partner.one",
                "KR",
                "Seoul",
                "Seoul",
                "SALES_PARTNER_APPROVED",
                4,
                new BigDecimal("55.00"),
                Instant.parse("2026-06-10T00:00:00Z")
        ));
        repository.partnerCount = 1;

        LeaderPartnerResponse response = service.getPartners(
                new AuthContext(10L, java.util.Set.of("KR")),
                "KR",
                null,
                null,
                null,
                0,
                20
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.page().totalItems()).isEqualTo(1);
    }

    private static class FakeRepository implements LeaderDashboardRepository {
        LeaderProfile profile;
        LeaderDashboardResponse.Kpis kpis = new LeaderDashboardResponse.Kpis(0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        List<LeaderPartnerResponse.PartnerSummary> partners = new ArrayList<>();
        long partnerCount;
        LocalDate lastPeriodStart;
        LocalDate lastPeriodEnd;

        @Override
        public Optional<LeaderProfile> findLeaderProfile(long leaderId) {
            return Optional.ofNullable(profile);
        }

        @Override
        public LeaderDashboardResponse.Kpis findKpis(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
            lastPeriodStart = periodStart;
            lastPeriodEnd = periodEnd;
            return kpis;
        }

        @Override
        public List<LeaderDashboardResponse.MonthlyVolume> findMonthlyVolume(
                long leaderId,
                String countryScope,
                LocalDate periodStart,
                LocalDate periodEnd
        ) {
            return List.of();
        }

        @Override
        public LeaderDashboardResponse.FeeSummary findFeeSummary(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
            return new LeaderDashboardResponse.FeeSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        @Override
        public List<LeaderPartnerResponse.PartnerSummary> findPartners(
                long leaderId,
                String countryScope,
                PartnerSearchCriteria criteria
        ) {
            return partners;
        }

        @Override
        public long countPartners(long leaderId, String countryScope, PartnerSearchCriteria criteria) {
            return partnerCount;
        }

        @Override
        public Map<String, Object> findSignupApplications(long leaderId, String countryScope, String applicantType) {
            return Map.of("rows", List.of());
        }

        @Override
        public Map<String, Object> findPartnerSales(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
            return Map.of("stats", List.of(), "t1Rows", List.of(), "merchantRows", List.of());
        }

        @Override
        public Map<String, Object> findMerchants(long leaderId, String countryScope) {
            return Map.of("stats", List.of(), "rows", List.of());
        }

        @Override
        public Map<String, Object> findMerchantSales(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
            return Map.of("stats", List.of(), "t1Rows", List.of(), "t2Rows", List.of());
        }

        @Override
        public Map<String, Object> findTransactions(long leaderId, String countryScope, String variant, LocalDate periodStart, LocalDate periodEnd) {
            return Map.of("stats", List.of(), "all", Map.of("rows", List.of()), "offline", Map.of("rows", List.of()), "failed", Map.of("rows", List.of()));
        }

        @Override
        public Map<String, Object> findSettlementRequestSummary(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
            return Map.of("stats", List.of());
        }

        @Override
        public Map<String, Object> findSettlementHistory(long leaderId, String countryScope) {
            return Map.of("rows", List.of());
        }

        @Override
        public Map<String, Object> findSettlementDetail(long leaderId, String countryScope) {
            return Map.of("basicInfo", List.of());
        }

        @Override
        public Map<String, Object> findHqNotices(long leaderId, String countryScope) {
            return Map.of("rows", List.of());
        }

        @Override
        public Map<String, Object> findNoticeSendSummary(long leaderId, String countryScope) {
            return Map.of("metrics", List.of());
        }

        @Override
        public Map<String, Object> findNoticeHistory(long leaderId, String countryScope) {
            return Map.of("metrics", List.of(), "rows", List.of());
        }

        @Override
        public Map<String, Object> findProfile(long leaderId, String countryScope) {
            return Map.of("code", "SP-00010");
        }

        @Override
        public Map<String, Object> findActivityLogs(long leaderId, String countryScope) {
            return Map.of("metrics", List.of(), "rows", List.of());
        }
    }
}
