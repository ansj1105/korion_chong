package com.korion.chong.merchant;

import jakarta.validation.constraints.Pattern;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/merchant")
public class MerchantController {
    private final MerchantAuthContextFactory authContextFactory;
    private final MerchantDashboardService service;

    public MerchantController(MerchantAuthContextFactory authContextFactory, MerchantDashboardService service) {
        this.authContextFactory = authContextFactory;
        this.service = service;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope,
            @RequestParam(required = false) @Pattern(regexp = "\\d{4}-\\d{2}") String period
    ) {
        return service.getDashboard(context(merchantId, countryScopes), countryScope, period);
    }

    @GetMapping({"/transactions", "/payments"})
    public Map<String, Object> transactions(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope,
            @RequestParam(defaultValue = "all") String variant,
            @RequestParam(required = false) @Pattern(regexp = "\\d{4}-\\d{2}") String period
    ) {
        return service.getTransactions(context(merchantId, countryScopes), countryScope, period, variant);
    }

    @GetMapping("/store")
    public Map<String, Object> store(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getStore(context(merchantId, countryScopes), countryScope);
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getProfile(context(merchantId, countryScopes), countryScope);
    }

    @GetMapping({"/hq-notices", "/notices"})
    public Map<String, Object> hqNotices(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getHqNotices(context(merchantId, countryScopes), countryScope);
    }

    @GetMapping("/activity-logs")
    public Map<String, Object> activityLogs(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getActivityLogs(context(merchantId, countryScopes), countryScope);
    }

    @GetMapping("/settlements")
    public Map<String, Object> settlementHistory(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getSettlementHistory(context(merchantId, countryScopes), countryScope);
    }

    @GetMapping("/settlements/detail")
    public Map<String, Object> settlementDetail(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getSettlementDetail(context(merchantId, countryScopes), countryScope);
    }

    private MerchantAuthContext context(String merchantId, String countryScopes) {
        return authContextFactory.fromHeaders(merchantId, countryScopes);
    }
}
