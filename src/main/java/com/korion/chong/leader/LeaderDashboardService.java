package com.korion.chong.leader;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
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
