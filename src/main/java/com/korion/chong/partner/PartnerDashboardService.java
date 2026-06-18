package com.korion.chong.partner;

import com.korion.chong.leader.ForbiddenCountryScopeException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PartnerDashboardService {
    private final PartnerDashboardRepository repository;
    private final Clock clock;

    public PartnerDashboardService(PartnerDashboardRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Map<String, Object> getDashboard(PartnerAuthContext authContext, String countryScope, String period) {
        loadAndAuthorize(authContext, countryScope);
        DateRange range = resolvePeriod(period);
        return repository.findDashboard(authContext.partnerId(), countryScope, range.start(), range.end());
    }

    public Map<String, Object> getMerchantApplications(PartnerAuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findMerchantApplications(authContext.partnerId(), countryScope);
    }

    public Map<String, Object> getMerchantApplicationDetail(PartnerAuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findMerchantApplicationDetail(authContext.partnerId(), countryScope);
    }

    public Map<String, Object> getMerchants(PartnerAuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findMerchants(authContext.partnerId(), countryScope);
    }

    public Map<String, Object> getMerchantSales(PartnerAuthContext authContext, String countryScope, String period) {
        loadAndAuthorize(authContext, countryScope);
        DateRange range = resolvePeriod(period);
        return repository.findMerchantSales(authContext.partnerId(), countryScope, range.start(), range.end());
    }

    public Map<String, Object> getSettlementRequestSummary(PartnerAuthContext authContext, String countryScope, String period) {
        loadAndAuthorize(authContext, countryScope);
        DateRange range = resolvePeriod(period);
        return repository.findSettlementRequestSummary(authContext.partnerId(), countryScope, range.start(), range.end());
    }

    public Map<String, Object> getSettlementHistory(PartnerAuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findSettlementHistory(authContext.partnerId(), countryScope);
    }

    public Map<String, Object> getSettlementDetail(PartnerAuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findSettlementDetail(authContext.partnerId(), countryScope);
    }

    public Map<String, Object> getHqNotices(PartnerAuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findHqNotices(authContext.partnerId(), countryScope);
    }

    public Map<String, Object> getNoticeSendSummary(PartnerAuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findNoticeSendSummary(authContext.partnerId(), countryScope);
    }

    public Map<String, Object> getNoticeHistory(PartnerAuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findNoticeHistory(authContext.partnerId(), countryScope);
    }

    public Map<String, Object> getProfile(PartnerAuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findProfile(authContext.partnerId(), countryScope);
    }

    public Map<String, Object> getActivityLogs(PartnerAuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findActivityLogs(authContext.partnerId(), countryScope);
    }

    private PartnerProfile loadAndAuthorize(PartnerAuthContext authContext, String countryScope) {
        PartnerProfile profile = repository.findPartnerProfile(authContext.partnerId())
                .orElseThrow(() -> new PartnerNotFoundException(authContext.partnerId()));
        if (!authContext.canAccess(countryScope) || !countryScope.equals(profile.countryScope())) {
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
