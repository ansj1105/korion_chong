package com.korion.chong.leader;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LeaderDashboardService {
    private final LeaderDashboardRepository repository;
    private final Clock clock;

    public LeaderDashboardService(LeaderDashboardRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public LeaderDashboardResponse getDashboard(AuthContext authContext, String period, String countryScope) {
        LeaderProfile profile = loadAndAuthorize(authContext, countryScope);
        DateRange range = resolvePeriod(period);
        LeaderDashboardResponse.Kpis kpis = repository.findKpis(authContext.leaderId(), countryScope, range.start(), range.end());

        return new LeaderDashboardResponse(
                profile,
                kpis,
                new LeaderDashboardResponse.OrganizationSummary(
                        repository.countPartners(authContext.leaderId(), countryScope, new PartnerSearchCriteria(null, null, null, 0, 1)),
                        kpis.approvedMerchantCount()
                ),
                repository.findMonthlyVolume(authContext.leaderId(), countryScope, range.start(), range.end()),
                repository.findFeeSummary(authContext.leaderId(), countryScope, range.start(), range.end()),
                java.util.List.of()
        );
    }

    public LeaderPartnerResponse getPartners(
            AuthContext authContext,
            String countryScope,
            String keyword,
            String status,
            String region,
            int page,
            int size
    ) {
        loadAndAuthorize(authContext, countryScope);
        PartnerSearchCriteria criteria = new PartnerSearchCriteria(keyword, status, region, page, size);
        return new LeaderPartnerResponse(
                repository.findPartners(authContext.leaderId(), countryScope, criteria),
                new LeaderPartnerResponse.PageMeta(page, size, repository.countPartners(authContext.leaderId(), countryScope, criteria))
        );
    }

    public Map<String, Object> getSignupApplications(AuthContext authContext, String countryScope, String applicantType) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findSignupApplications(authContext.leaderId(), countryScope, applicantType);
    }

    public Map<String, Object> getPartnerSales(AuthContext authContext, String countryScope, String period) {
        loadAndAuthorize(authContext, countryScope);
        DateRange range = resolvePeriod(period);
        return repository.findPartnerSales(authContext.leaderId(), countryScope, range.start(), range.end());
    }

    public Map<String, Object> getMerchants(AuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findMerchants(authContext.leaderId(), countryScope);
    }

    public Map<String, Object> getMerchantSales(AuthContext authContext, String countryScope, String period) {
        loadAndAuthorize(authContext, countryScope);
        DateRange range = resolvePeriod(period);
        return repository.findMerchantSales(authContext.leaderId(), countryScope, range.start(), range.end());
    }

    public Map<String, Object> getTransactions(AuthContext authContext, String countryScope, String variant, String period) {
        loadAndAuthorize(authContext, countryScope);
        DateRange range = resolvePeriod(period);
        return repository.findTransactions(authContext.leaderId(), countryScope, variant, range.start(), range.end());
    }

    public Map<String, Object> getSettlementRequestSummary(AuthContext authContext, String countryScope, String period) {
        loadAndAuthorize(authContext, countryScope);
        DateRange range = resolvePeriod(period);
        return repository.findSettlementRequestSummary(authContext.leaderId(), countryScope, range.start(), range.end());
    }

    public Map<String, Object> getSettlementHistory(AuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findSettlementHistory(authContext.leaderId(), countryScope);
    }

    public Map<String, Object> getSettlementDetail(AuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findSettlementDetail(authContext.leaderId(), countryScope);
    }

    public Map<String, Object> getHqNotices(AuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findHqNotices(authContext.leaderId(), countryScope);
    }

    public Map<String, Object> getNoticeSendSummary(AuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findNoticeSendSummary(authContext.leaderId(), countryScope);
    }

    public Map<String, Object> getNoticeHistory(AuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findNoticeHistory(authContext.leaderId(), countryScope);
    }

    public Map<String, Object> getProfile(AuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findProfile(authContext.leaderId(), countryScope);
    }

    public Map<String, Object> getActivityLogs(AuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findActivityLogs(authContext.leaderId(), countryScope);
    }

    private LeaderProfile loadAndAuthorize(AuthContext authContext, String countryScope) {
        LeaderProfile profile = repository.findLeaderProfile(authContext.leaderId())
                .orElseThrow(() -> new LeaderNotFoundException(authContext.leaderId()));
        if (!authContext.canAccess(countryScope) || !profile.countryScopes().contains(countryScope)) {
            throw new ForbiddenCountryScopeException(countryScope);
        }
        return profile;
    }

    private DateRange resolvePeriod(String period) {
        YearMonth month = period == null || period.isBlank()
                ? YearMonth.now(clock)
                : YearMonth.parse(period);
        return new DateRange(month.atDay(1), month.plusMonths(1).atDay(1));
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }
}
