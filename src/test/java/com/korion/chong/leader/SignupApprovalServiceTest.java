package com.korion.chong.leader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SignupApprovalServiceTest {
    private final FakeSignupApprovalRepository repository = new FakeSignupApprovalRepository();
    private final FakeLeaderRepository leaderRepository = new FakeLeaderRepository();
    private final SignupApprovalService service = new SignupApprovalService(
            repository,
            leaderRepository,
            Clock.fixed(Instant.parse("2026-06-18T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void approvePartnerCreatesUserPartnerContractAndWallet() {
        leaderRepository.profile = new LeaderProfile(10L, 100L, "leader.kr", "COUNTRY_LEADER_APPROVED", List.of("KR"));
        repository.application = application("PARTNER", "REQUESTED", 10L, "COUNTRY_LEADER", null);

        SignupApprovalResponse response = service.approve(
                new AuthContext(10L, java.util.Set.of("KR")),
                123L,
                new SignupApprovalDecisionRequest("ok", "req-approve")
        );

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.userId()).isEqualTo(500L);
        assertThat(response.partnerId()).isEqualTo(600L);
        assertThat(response.contractId()).isEqualTo(700L);
        assertThat(response.walletAddressId()).isEqualTo(800L);
        assertThat(repository.approved).isTrue();
        assertThat(repository.activityActionType).isEqualTo("SIGNUP_APPLICATION_APPROVED");
    }

    @Test
    void approveMerchantUnderSalesPartnerCreatesMerchantUnderOwningLeader() {
        leaderRepository.profile = new LeaderProfile(10L, 100L, "leader.kr", "COUNTRY_LEADER_APPROVED", List.of("KR"));
        repository.application = application("MERCHANT", "REVIEWING", 20L, "SALES_PARTNER", 10L);

        SignupApprovalResponse response = service.approve(
                new AuthContext(10L, java.util.Set.of("KR")),
                123L,
                new SignupApprovalDecisionRequest("ok", "req-approve")
        );

        assertThat(response.merchantId()).isEqualTo(650L);
        assertThat(repository.lastLeaderPartnerId).isEqualTo(10L);
        assertThat(repository.lastSalesPartnerId).isEqualTo(20L);
    }

    @Test
    void rejectApprovedApplicationIsBlocked() {
        leaderRepository.profile = new LeaderProfile(10L, 100L, "leader.kr", "COUNTRY_LEADER_APPROVED", List.of("KR"));
        repository.application = application("PARTNER", "APPROVED", 10L, "COUNTRY_LEADER", null);

        assertThatThrownBy(() -> service.reject(
                new AuthContext(10L, java.util.Set.of("KR")),
                123L,
                new SignupApprovalDecisionRequest("duplicate", "req-reject")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not decisionable");
    }

    private SignupApplicationApprovalRecord application(String applicantType, String status, Long ownerPartnerId, String ownerPartnerType, Long ownerLeaderId) {
        return new SignupApplicationApprovalRecord(
                123L,
                applicantType,
                applicantType.toLowerCase() + "01",
                "hash",
                applicantType.toLowerCase() + "@example.com",
                applicantType + " Co",
                applicantType + " Owner",
                "010",
                "REF",
                ownerPartnerId,
                ownerPartnerType,
                ownerLeaderId,
                "KR",
                "Seoul",
                "Seoul",
                "address",
                "Retail",
                "TRON",
                "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb",
                "VERIFIED",
                ownerPartnerType.equals("SALES_PARTNER") ? "PARTNER_DIRECT" : "LEADER_DIRECT",
                status
        );
    }

    private static class FakeSignupApprovalRepository implements SignupApprovalRepository {
        SignupApplicationApprovalRecord application;
        boolean approved;
        String activityActionType;
        Long lastLeaderPartnerId;
        Long lastSalesPartnerId;

        @Override
        public Optional<SignupApplicationApprovalRecord> findApplicationForUpdate(long applicationId) {
            return Optional.ofNullable(application);
        }

        @Override
        public long createUser(SignupApplicationApprovalRecord application) {
            return 500L;
        }

        @Override
        public long createPartner(SignupApplicationApprovalRecord application, long userId, long approvedByUserId, Long parentPartnerId, Instant now) {
            lastLeaderPartnerId = parentPartnerId;
            return 600L;
        }

        @Override
        public long createMerchant(SignupApplicationApprovalRecord application, long userId, long approvedByUserId, Long leaderPartnerId, Long salesPartnerId, Instant now) {
            lastLeaderPartnerId = leaderPartnerId;
            lastSalesPartnerId = salesPartnerId;
            return 650L;
        }

        @Override
        public long createContract(SignupApplicationApprovalRecord application, long approvedByUserId, Long leaderPartnerId, Long salesPartnerId, Long merchantId, Instant now) {
            return 700L;
        }

        @Override
        public Long createWalletAddress(SignupApplicationApprovalRecord application, long userId, Long partnerId, Long merchantId, Instant now) {
            return 800L;
        }

        @Override
        public void approveApplication(long applicationId, Instant now) {
            approved = true;
        }

        @Override
        public void rejectApplication(long applicationId, String reason, Instant now) {
        }

        @Override
        public long findUserIdByLeaderId(long leaderId) {
            return 100L;
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
        public LeaderDashboardResponse.Kpis findKpis(long leaderId, String countryScope, java.time.LocalDate periodStart, java.time.LocalDate periodEnd) {
            return null;
        }

        @Override
        public List<LeaderDashboardResponse.MonthlyVolume> findMonthlyVolume(long leaderId, String countryScope, java.time.LocalDate periodStart, java.time.LocalDate periodEnd) {
            return List.of();
        }

        @Override
        public LeaderDashboardResponse.FeeSummary findFeeSummary(long leaderId, String countryScope, java.time.LocalDate periodStart, java.time.LocalDate periodEnd) {
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
        public java.util.Map<String, Object> findSignupApplications(long leaderId, String countryScope, String applicantType) {
            return java.util.Map.of();
        }

        @Override
        public java.util.Map<String, Object> findPartnerSales(long leaderId, String countryScope, java.time.LocalDate periodStart, java.time.LocalDate periodEnd) {
            return java.util.Map.of();
        }

        @Override
        public java.util.Map<String, Object> findMerchants(long leaderId, String countryScope) {
            return java.util.Map.of();
        }

        @Override
        public java.util.Map<String, Object> findMerchantSales(long leaderId, String countryScope, java.time.LocalDate periodStart, java.time.LocalDate periodEnd) {
            return java.util.Map.of();
        }

        @Override
        public java.util.Map<String, Object> findTransactions(long leaderId, String countryScope, String variant, java.time.LocalDate periodStart, java.time.LocalDate periodEnd) {
            return java.util.Map.of();
        }

        @Override
        public java.util.Map<String, Object> findSettlementRequestSummary(long leaderId, String countryScope, java.time.LocalDate periodStart, java.time.LocalDate periodEnd) {
            return java.util.Map.of();
        }

        @Override
        public java.util.Map<String, Object> findSettlementHistory(long leaderId, String countryScope) {
            return java.util.Map.of();
        }

        @Override
        public java.util.Map<String, Object> findSettlementDetail(long leaderId, String countryScope) {
            return java.util.Map.of();
        }

        @Override
        public java.util.Map<String, Object> findHqNotices(long leaderId, String countryScope) {
            return java.util.Map.of();
        }

        @Override
        public java.util.Map<String, Object> findNoticeSendSummary(long leaderId, String countryScope) {
            return java.util.Map.of();
        }

        @Override
        public java.util.Map<String, Object> findNoticeHistory(long leaderId, String countryScope) {
            return java.util.Map.of();
        }

        @Override
        public java.util.Map<String, Object> findProfile(long leaderId, String countryScope) {
            return java.util.Map.of();
        }

        @Override
        public java.util.Map<String, Object> findActivityLogs(long leaderId, String countryScope) {
            return java.util.Map.of();
        }
    }
}
