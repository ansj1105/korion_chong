package com.korion.chong.leader;

import com.korion.chong.settlement.SettlementActionRequest;
import com.korion.chong.settlement.SettlementActionResponse;
import com.korion.chong.settlement.SettlementCreateRequest;
import com.korion.chong.settlement.SettlementService;
import com.korion.chong.notice.NoticeSendRequest;
import com.korion.chong.notice.NoticeSendResponse;
import com.korion.chong.notice.NoticeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final SignupApprovalService signupApprovalService;
    private final SettlementService settlementService;
    private final NoticeService noticeService;

    public LeaderController(
            AuthContextFactory authContextFactory,
            LeaderDashboardService service,
            SignupApprovalService signupApprovalService,
            SettlementService settlementService,
            NoticeService noticeService
    ) {
        this.authContextFactory = authContextFactory;
        this.service = service;
        this.signupApprovalService = signupApprovalService;
        this.settlementService = settlementService;
        this.noticeService = noticeService;
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

    @GetMapping({"/partner-applications", "/partner-requests"})
    public Map<String, Object> partnerApplications(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getSignupApplications(context(leaderId, countryScopes), countryScope, "PARTNER");
    }

    @GetMapping({"/merchant-applications", "/merchant-requests"})
    public Map<String, Object> merchantApplications(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getSignupApplications(context(leaderId, countryScopes), countryScope, "MERCHANT");
    }

    @GetMapping("/partner-sales")
    public Map<String, Object> partnerSales(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope,
            @RequestParam(required = false) @Pattern(regexp = "\\d{4}-\\d{2}") String period
    ) {
        return service.getPartnerSales(context(leaderId, countryScopes), countryScope, period);
    }

    @GetMapping("/merchants")
    public Map<String, Object> merchants(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getMerchants(context(leaderId, countryScopes), countryScope);
    }

    @GetMapping("/merchant-sales")
    public Map<String, Object> merchantSales(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope,
            @RequestParam(required = false) @Pattern(regexp = "\\d{4}-\\d{2}") String period
    ) {
        return service.getMerchantSales(context(leaderId, countryScopes), countryScope, period);
    }

    @GetMapping("/transactions")
    public Map<String, Object> transactions(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope,
            @RequestParam(defaultValue = "all") String variant,
            @RequestParam(required = false) @Pattern(regexp = "\\d{4}-\\d{2}") String period
    ) {
        return service.getTransactions(context(leaderId, countryScopes), countryScope, variant, period);
    }

    @GetMapping("/settlements/request-summary")
    public Map<String, Object> settlementRequestSummary(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope,
            @RequestParam(required = false) @Pattern(regexp = "\\d{4}-\\d{2}") String period
    ) {
        return service.getSettlementRequestSummary(context(leaderId, countryScopes), countryScope, period);
    }

    @GetMapping({"/settlements", "/settlement-history"})
    public Map<String, Object> settlementHistory(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getSettlementHistory(context(leaderId, countryScopes), countryScope);
    }

    @GetMapping({"/settlements/detail", "/settlement-history/{settlementRequestId}"})
    public Map<String, Object> settlementDetail(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getSettlementDetail(context(leaderId, countryScopes), countryScope);
    }

    @GetMapping("/hq-notices")
    public Map<String, Object> hqNotices(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getHqNotices(context(leaderId, countryScopes), countryScope);
    }

    @GetMapping("/notices/send-summary")
    public Map<String, Object> noticeSendSummary(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getNoticeSendSummary(context(leaderId, countryScopes), countryScope);
    }

    @GetMapping({"/notices", "/notices/history"})
    public Map<String, Object> noticeHistory(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getNoticeHistory(context(leaderId, countryScopes), countryScope);
    }

    @PostMapping("/notices/send")
    public NoticeSendResponse sendNotice(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @Valid @RequestBody NoticeSendRequest request
    ) {
        return noticeService.sendLeaderNotice(context(leaderId, countryScopes), request);
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getProfile(context(leaderId, countryScopes), countryScope);
    }

    @GetMapping("/activity-logs")
    public Map<String, Object> activityLogs(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @RequestParam String countryScope
    ) {
        return service.getActivityLogs(context(leaderId, countryScopes), countryScope);
    }

    @PostMapping("/signup-applications/{applicationId}/approve")
    public SignupApprovalResponse approveSignupApplication(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @PathVariable long applicationId,
            @Valid @RequestBody(required = false) SignupApprovalDecisionRequest request
    ) {
        return signupApprovalService.approve(context(leaderId, countryScopes), applicationId, request);
    }

    @PostMapping("/signup-applications/{applicationId}/reject")
    public SignupRejectionResponse rejectSignupApplication(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @PathVariable long applicationId,
            @Valid @RequestBody(required = false) SignupApprovalDecisionRequest request
    ) {
        return signupApprovalService.reject(context(leaderId, countryScopes), applicationId, request);
    }

    @PostMapping("/settlement-requests")
    public SettlementActionResponse createSettlementRequest(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @Valid @RequestBody SettlementCreateRequest request
    ) {
        return settlementService.createLeaderRequest(context(leaderId, countryScopes), request);
    }

    @PostMapping("/settlement-requests/{settlementRequestId}/approve")
    public SettlementActionResponse approveSettlementRequest(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @PathVariable long settlementRequestId,
            @Valid @RequestBody SettlementActionRequest request
    ) {
        return settlementService.approve(context(leaderId, countryScopes), settlementRequestId, request);
    }

    @PostMapping("/settlement-requests/{settlementRequestId}/reject")
    public SettlementActionResponse rejectSettlementRequest(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @PathVariable long settlementRequestId,
            @Valid @RequestBody SettlementActionRequest request
    ) {
        return settlementService.reject(context(leaderId, countryScopes), settlementRequestId, request);
    }

    @PostMapping("/settlement-requests/{settlementRequestId}/mark-paid")
    public SettlementActionResponse markSettlementRequestPaid(
            @RequestHeader("X-Leader-Id") String leaderId,
            @RequestHeader("X-Country-Scopes") String countryScopes,
            @PathVariable long settlementRequestId,
            @Valid @RequestBody(required = false) SettlementActionRequest request
    ) {
        return settlementService.markPaid(context(leaderId, countryScopes), settlementRequestId, request);
    }

    private AuthContext context(String leaderId, String countryScopes) {
        return authContextFactory.fromHeaders(leaderId, countryScopes);
    }
}
