package com.korion.chong.merchant;

import com.korion.chong.leader.ForbiddenCountryScopeException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MerchantDashboardService {
    private final MerchantDashboardRepository repository;
    private final Clock clock;

    public MerchantDashboardService(MerchantDashboardRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Map<String, Object> getDashboard(MerchantAuthContext authContext, String countryScope, String period) {
        loadAndAuthorize(authContext, countryScope);
        DateRange range = resolvePeriod(period);
        return repository.findDashboard(authContext.merchantId(), countryScope, range.start(), range.end());
    }

    public Map<String, Object> getTransactions(MerchantAuthContext authContext, String countryScope, String period, String variant) {
        loadAndAuthorize(authContext, countryScope);
        DateRange range = resolvePeriod(period);
        return repository.findTransactions(authContext.merchantId(), countryScope, range.start(), range.end(), variant);
    }

    public Map<String, Object> getStore(MerchantAuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findStore(authContext.merchantId(), countryScope);
    }

    public Map<String, Object> getProfile(MerchantAuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findProfile(authContext.merchantId(), countryScope);
    }

    public Map<String, Object> getHqNotices(MerchantAuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findHqNotices(authContext.merchantId(), countryScope);
    }

    public Map<String, Object> getActivityLogs(MerchantAuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findActivityLogs(authContext.merchantId(), countryScope);
    }

    public Map<String, Object> getSettlementHistory(MerchantAuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findSettlementHistory(authContext.merchantId(), countryScope);
    }

    public Map<String, Object> getSettlementDetail(MerchantAuthContext authContext, String countryScope) {
        loadAndAuthorize(authContext, countryScope);
        return repository.findSettlementDetail(authContext.merchantId(), countryScope);
    }

    private MerchantProfile loadAndAuthorize(MerchantAuthContext authContext, String countryScope) {
        MerchantProfile profile = repository.findMerchantProfile(authContext.merchantId())
                .orElseThrow(() -> new MerchantNotFoundException(authContext.merchantId()));
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
