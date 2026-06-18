package com.korion.chong.partner;

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
@RequestMapping("/api/partner")
public class PartnerController {
    private final PartnerAuthContextFactory authContextFactory;
    private final PartnerDashboardService service;

    public PartnerController(PartnerAuthContextFactory authContextFactory, PartnerDashboardService service) {
        this.authContextFactory = authContextFactory;
        this.service = service;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(
            @RequestHeader("X-Partner-Id") String partnerId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope,
            @RequestParam(required = false) @Pattern(regexp = "\\d{4}-\\d{2}") String period
    ) {
        return service.getDashboard(context(partnerId, countryScopes), countryScope, period);
    }

    @GetMapping("/merchant-applications")
    public Map<String, Object> merchantApplications(
            @RequestHeader("X-Partner-Id") String partnerId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getMerchantApplications(context(partnerId, countryScopes), countryScope);
    }

    @GetMapping("/merchant-applications/detail")
    public Map<String, Object> merchantApplicationDetail(
            @RequestHeader("X-Partner-Id") String partnerId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getMerchantApplicationDetail(context(partnerId, countryScopes), countryScope);
    }

    @GetMapping("/merchants")
    public Map<String, Object> merchants(
            @RequestHeader("X-Partner-Id") String partnerId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getMerchants(context(partnerId, countryScopes), countryScope);
    }

    @GetMapping("/merchant-sales")
    public Map<String, Object> merchantSales(
            @RequestHeader("X-Partner-Id") String partnerId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope,
            @RequestParam(required = false) @Pattern(regexp = "\\d{4}-\\d{2}") String period
    ) {
        return service.getMerchantSales(context(partnerId, countryScopes), countryScope, period);
    }

    @GetMapping("/settlements/request-summary")
    public Map<String, Object> settlementRequestSummary(
            @RequestHeader("X-Partner-Id") String partnerId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope,
            @RequestParam(required = false) @Pattern(regexp = "\\d{4}-\\d{2}") String period
    ) {
        return service.getSettlementRequestSummary(context(partnerId, countryScopes), countryScope, period);
    }

    @GetMapping("/settlements")
    public Map<String, Object> settlementHistory(
            @RequestHeader("X-Partner-Id") String partnerId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getSettlementHistory(context(partnerId, countryScopes), countryScope);
    }

    @GetMapping("/settlements/detail")
    public Map<String, Object> settlementDetail(
            @RequestHeader("X-Partner-Id") String partnerId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getSettlementDetail(context(partnerId, countryScopes), countryScope);
    }

    @GetMapping("/hq-notices")
    public Map<String, Object> hqNotices(
            @RequestHeader("X-Partner-Id") String partnerId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getHqNotices(context(partnerId, countryScopes), countryScope);
    }

    @GetMapping("/notices/send-summary")
    public Map<String, Object> noticeSendSummary(
            @RequestHeader("X-Partner-Id") String partnerId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getNoticeSendSummary(context(partnerId, countryScopes), countryScope);
    }

    @GetMapping({"/notices", "/notices/history"})
    public Map<String, Object> noticeHistory(
            @RequestHeader("X-Partner-Id") String partnerId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getNoticeHistory(context(partnerId, countryScopes), countryScope);
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(
            @RequestHeader("X-Partner-Id") String partnerId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getProfile(context(partnerId, countryScopes), countryScope);
    }

    @GetMapping("/activity-logs")
    public Map<String, Object> activityLogs(
            @RequestHeader("X-Partner-Id") String partnerId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getActivityLogs(context(partnerId, countryScopes), countryScope);
    }

    private PartnerAuthContext context(String partnerId, String countryScopes) {
        return authContextFactory.fromHeaders(partnerId, countryScopes);
    }
}
