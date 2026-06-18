package com.korion.chong.leader;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/leader")
public class LeaderController {
    private final AuthContextFactory authContextFactory;
    private final LeaderDashboardService service;

    public LeaderController(AuthContextFactory authContextFactory, LeaderDashboardService service) {
        this.authContextFactory = authContextFactory;
        this.service = service;
    }

    @GetMapping("/dashboard")
    public LeaderDashboardResponse dashboard(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam(required = false) @Pattern(regexp = "\\d{4}-\\d{2}") String period,
            @RequestParam String countryScope
    ) {
        AuthContext authContext = authContextFactory.fromHeaders(leaderId, countryScopes);
        return service.getDashboard(authContext, period, countryScope);
    }

    @GetMapping("/partners")
    public LeaderPartnerResponse partners(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        AuthContext authContext = authContextFactory.fromHeaders(leaderId, countryScopes);
        return service.getPartners(authContext, countryScope, keyword, status, region, page, size);
    }
}
