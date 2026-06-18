package com.korion.chong.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.korion.chong.leader.AuthContext;
import com.korion.chong.leader.ForbiddenCountryScopeException;
import com.korion.chong.leader.LeaderDashboardRepository;
import com.korion.chong.leader.LeaderDashboardResponse;
import com.korion.chong.leader.LeaderPartnerResponse;
import com.korion.chong.leader.LeaderProfile;
import com.korion.chong.leader.PartnerSearchCriteria;
import com.korion.chong.partner.PartnerAuthContext;
import com.korion.chong.partner.PartnerDashboardRepository;
import com.korion.chong.partner.PartnerProfile;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SettlementServiceTest {
    private final FakeSettlementRepository repository = new FakeSettlementRepository();
    private final FakeLeaderRepository leaderRepository = new FakeLeaderRepository();
    private final FakePartnerRepository partnerRepository = new FakePartnerRepository();
    private final SettlementService service = new SettlementService(
            repository,
            leaderRepository,
            partnerRepository,
            Clock.fixed(Instant.parse("2026-06-18T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void partnerRequestCannotExceedAvailableAmount() {
        partnerRepository.profile = new PartnerProfile(20L, 200L, "partner.kr", "SALES_PARTNER_APPROVED", "KR", "Seoul", "Seoul");
        repository.available = new BigDecimal("50.00");

        assertThatThrownBy(() -> service.createPartnerRequest(
                new PartnerAuthContext(20L, java.util.Set.of("KR")),
                createRequest(new BigDecimal("100.00"))
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("available");
    }

    @Test
    void partnerRequestCreatesAndAttachesCommissions() {
        partnerRepository.profile = new PartnerProfile(20L, 200L, "partner.kr", "SALES_PARTNER_APPROVED", "KR", "Seoul", "Seoul");
        repository.available = new BigDecimal("150.00");

        SettlementActionResponse response = service.createPartnerRequest(
                new PartnerAuthContext(20L, java.util.Set.of("KR")),
                createRequest(new BigDecimal("100.00"))
        );

        assertThat(response.status()).isEqualTo("REQUESTED");
        assertThat(repository.attachedPartnerId).isEqualTo(20L);
        assertThat(repository.activityActionType).isEqualTo("SETTLEMENT_REQUEST_CREATED");
    }

    @Test
    void leaderApprovesOnlyManageableRequestedSettlement() {
        leaderRepository.profile = new LeaderProfile(10L, 100L, "leader.kr", "COUNTRY_LEADER_APPROVED", List.of("KR"));
        repository.record = settlement("REQUESTED");
        repository.manageable = true;

        SettlementActionResponse response = service.approve(
                new AuthContext(10L, java.util.Set.of("KR")),
                101L,
                new SettlementActionRequest(new BigDecimal("90.00"), "ok", "req-approve")
        );

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.approvedAmount()).isEqualByComparingTo("90.00");
    }

    @Test
    void leaderCannotMarkUnapprovedSettlementPaid() {
        leaderRepository.profile = new LeaderProfile(10L, 100L, "leader.kr", "COUNTRY_LEADER_APPROVED", List.of("KR"));
        repository.record = settlement("REQUESTED");
        repository.manageable = true;

        assertThatThrownBy(() -> service.markPaid(
                new AuthContext(10L, java.util.Set.of("KR")),
                101L,
                new SettlementActionRequest(null, null, "req-paid")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    void leaderCannotManageForeignSettlement() {
        leaderRepository.profile = new LeaderProfile(10L, 100L, "leader.kr", "COUNTRY_LEADER_APPROVED", List.of("KR"));
        repository.record = settlement("REQUESTED");
        repository.manageable = false;

        assertThatThrownBy(() -> service.reject(
                new AuthContext(10L, java.util.Set.of("KR")),
                101L,
                new SettlementActionRequest(null, "foreign", "req-reject")
        )).isInstanceOf(ForbiddenCountryScopeException.class);
    }

    private SettlementCreateRequest createRequest(BigDecimal amount) {
        return new SettlementCreateRequest(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 10),
                amount,
                900L,
                "memo",
                "req-settle"
        );
    }

    private SettlementRequestRecord settlement(String status) {
        return new SettlementRequestRecord(
                101L,
                "SET-101",
                200L,
                "PARTNER",
                20L,
                null,
                900L,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 10),
                "KORI",
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                status
        );
    }

    private static class FakeSettlementRepository implements SettlementRepository {
        BigDecimal available = BigDecimal.ZERO;
        SettlementRequestRecord record;
        boolean manageable;
        Long attachedPartnerId;
        String activityActionType;

        @Override
        public BigDecimal availablePartnerAmount(long partnerId, LocalDate periodStart, LocalDate periodEnd) {
            return available;
        }

        @Override
        public long createPartnerRequest(long requesterUserId, String recipientType, long recipientPartnerId, Long walletAddressId, LocalDate periodStart, LocalDate periodEnd, BigDecimal requestedAmount, String memo, Instant now) {
            record = new SettlementRequestRecord(101L, "SET-101", requesterUserId, recipientType, recipientPartnerId, null, walletAddressId, periodStart, periodEnd, "KORI", requestedAmount, BigDecimal.ZERO, BigDecimal.ZERO, "REQUESTED");
            return 101L;
        }

        @Override
        public void attachPartnerCommissions(long settlementRequestId, long partnerId, LocalDate periodStart, LocalDate periodEnd) {
            attachedPartnerId = partnerId;
        }

        @Override
        public Optional<SettlementRequestRecord> findForUpdate(long settlementRequestId) {
            return Optional.ofNullable(record);
        }

        @Override
        public boolean canLeaderManage(long leaderPartnerId, SettlementRequestRecord request) {
            return manageable;
        }

        @Override
        public SettlementRequestRecord approve(long settlementRequestId, long reviewedByUserId, BigDecimal approvedAmount, BigDecimal heldAmount, String note, Instant now) {
            record = new SettlementRequestRecord(record.id(), record.requestNo(), record.requesterUserId(), record.recipientType(), record.recipientPartnerId(), record.recipientMerchantId(), record.walletAddressId(), record.periodStart(), record.periodEnd(), record.currency(), record.requestedAmount(), approvedAmount, heldAmount, "APPROVED");
            return record;
        }

        @Override
        public SettlementRequestRecord reject(long settlementRequestId, long reviewedByUserId, String note, Instant now) {
            record = new SettlementRequestRecord(record.id(), record.requestNo(), record.requesterUserId(), record.recipientType(), record.recipientPartnerId(), record.recipientMerchantId(), record.walletAddressId(), record.periodStart(), record.periodEnd(), record.currency(), record.requestedAmount(), record.approvedAmount(), record.heldAmount(), "REJECTED");
            return record;
        }

        @Override
        public SettlementRequestRecord markPaid(long settlementRequestId, long reviewedByUserId, Instant now) {
            record = new SettlementRequestRecord(record.id(), record.requestNo(), record.requesterUserId(), record.recipientType(), record.recipientPartnerId(), record.recipientMerchantId(), record.walletAddressId(), record.periodStart(), record.periodEnd(), record.currency(), record.requestedAmount(), record.approvedAmount(), record.heldAmount(), "PAID");
            return record;
        }

        @Override
        public void recordActivity(String role, String actionType, String status, String targetType, Long targetId, String requestId) {
            activityActionType = actionType;
        }
    }

    private static class FakeLeaderRepository implements LeaderDashboardRepository {
        LeaderProfile profile;

        @Override
        public Optional<LeaderProfile> findLeaderProfile(long leaderId) {
            return Optional.ofNullable(profile);
        }

        @Override
        public LeaderDashboardResponse.Kpis findKpis(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
            return null;
        }

        @Override
        public List<LeaderDashboardResponse.MonthlyVolume> findMonthlyVolume(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
            return List.of();
        }

        @Override
        public LeaderDashboardResponse.FeeSummary findFeeSummary(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
            return null;
        }

        @Override
        public List<LeaderPartnerResponse.PartnerSummary> findPartners(long leaderId, String countryScope, PartnerSearchCriteria criteria) {
            return List.of();
        }

        @Override
        public long countPartners(long leaderId, String countryScope, PartnerSearchCriteria criteria) {
            return 0;
        }

        @Override
        public Map<String, Object> findSignupApplications(long leaderId, String countryScope, String applicantType) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findPartnerSales(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findMerchants(long leaderId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findMerchantSales(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findTransactions(long leaderId, String countryScope, String variant, LocalDate periodStart, LocalDate periodEnd) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findSettlementRequestSummary(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findSettlementHistory(long leaderId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findSettlementDetail(long leaderId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findHqNotices(long leaderId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findNoticeSendSummary(long leaderId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findNoticeHistory(long leaderId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findProfile(long leaderId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findActivityLogs(long leaderId, String countryScope) {
            return Map.of();
        }
    }

    private static class FakePartnerRepository implements PartnerDashboardRepository {
        PartnerProfile profile;

        @Override
        public Optional<PartnerProfile> findPartnerProfile(long partnerId) {
            return Optional.ofNullable(profile);
        }

        @Override
        public Map<String, Object> findDashboard(long partnerId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findMerchantApplications(long partnerId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findMerchantApplicationDetail(long partnerId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findMerchants(long partnerId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findMerchantSales(long partnerId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findSettlementRequestSummary(long partnerId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findSettlementHistory(long partnerId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findSettlementDetail(long partnerId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findHqNotices(long partnerId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findNoticeSendSummary(long partnerId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findNoticeHistory(long partnerId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findProfile(long partnerId, String countryScope) {
            return Map.of();
        }

        @Override
        public Map<String, Object> findActivityLogs(long partnerId, String countryScope) {
            return Map.of();
        }
    }
}
