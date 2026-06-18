package com.korion.chong.leader;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.korion.chong.api.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LeaderControllerContractTest {
    private LeaderDashboardService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(LeaderDashboardService.class);
        LeaderController controller = new LeaderController(new AuthContextFactory(), service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void dashboardReturnsContractShape() throws Exception {
        when(service.getDashboard(any(AuthContext.class), eq("2026-06"), eq("KR")))
                .thenReturn(new LeaderDashboardResponse(
                        new LeaderProfile(10L, 100L, "leader.kr", "COUNTRY_LEADER_APPROVED", List.of("KR")),
                        new LeaderDashboardResponse.Kpis(1, 2, new BigDecimal("30.00"), new BigDecimal("7.50")),
                        new LeaderDashboardResponse.OrganizationSummary(1, 2),
                        List.of(),
                        new LeaderDashboardResponse.FeeSummary(new BigDecimal("7.50"), BigDecimal.ZERO, new BigDecimal("15.00")),
                        List.of()
                ));

        mockMvc.perform(get("/api/leader/dashboard")
                        .header("X-Leader-Id", "10")
                        .header("X-Country-Scopes", "KR")
                        .queryParam("period", "2026-06")
                        .queryParam("countryScope", "KR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaderProfile.leaderId", equalTo(10)))
                .andExpect(jsonPath("$.kpis.approvedPartnerCount", equalTo(1)))
                .andExpect(jsonPath("$.organizationSummary.merchantCount", equalTo(2)));
    }

    @Test
    void partnersReturnsContractShape() throws Exception {
        when(service.getPartners(
                any(AuthContext.class),
                eq("KR"),
                eq("partner"),
                eq("SALES_PARTNER_APPROVED"),
                eq("Seoul"),
                eq(0),
                eq(20)
        )).thenReturn(new LeaderPartnerResponse(
                List.of(new LeaderPartnerResponse.PartnerSummary(
                        20L,
                        200L,
                        "partner.kr",
                        "KR",
                        "Seoul",
                        "Gangnam",
                        "SALES_PARTNER_APPROVED",
                        3,
                        new BigDecimal("1200.50"),
                        Instant.parse("2026-06-18T00:00:00Z")
                )),
                new LeaderPartnerResponse.PageMeta(0, 20, 1)
        ));

        mockMvc.perform(get("/api/leader/partners")
                        .header("X-Leader-Id", "10")
                        .header("X-Country-Scopes", "KR")
                        .queryParam("countryScope", "KR")
                        .queryParam("keyword", "partner")
                        .queryParam("status", "SALES_PARTNER_APPROVED")
                        .queryParam("region", "Seoul")
                        .queryParam("page", "0")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].partnerId", equalTo(20)))
                .andExpect(jsonPath("$.items[0].loginId", equalTo("partner.kr")))
                .andExpect(jsonPath("$.items[0].merchantCount", equalTo(3)))
                .andExpect(jsonPath("$.page.totalItems", equalTo(1)));
    }

    @Test
    void leaderPageEndpointsReturnFrontendContractShapes() throws Exception {
        when(service.getSignupApplications(any(AuthContext.class), eq("KR"), eq("PARTNER")))
                .thenReturn(Map.of("stats", List.of(Map.of("id", "active", "labelKey", "partner.kpi.active", "value", "1")), "rows", List.of()));
        when(service.getSignupApplications(any(AuthContext.class), eq("KR"), eq("MERCHANT")))
                .thenReturn(Map.of("stats", List.of(Map.of("id", "active", "labelKey", "merchant.kpi.active", "value", "1")), "rows", List.of()));
        when(service.getPartnerSales(any(AuthContext.class), eq("KR"), eq(null)))
                .thenReturn(Map.of("stats", List.of(), "t1Rows", List.of(), "merchantRows", List.of()));
        when(service.getMerchants(any(AuthContext.class), eq("KR")))
                .thenReturn(Map.of("stats", List.of(), "rows", List.of(Map.of("merchantCode", "MER-00001"))));
        when(service.getMerchantSales(any(AuthContext.class), eq("KR"), eq(null)))
                .thenReturn(Map.of("stats", List.of(), "t1Rows", List.of(), "t2Rows", List.of()));
        when(service.getTransactions(any(AuthContext.class), eq("KR"), eq("all"), eq(null)))
                .thenReturn(Map.of("stats", List.of(), "all", Map.of("rows", List.of()), "offline", Map.of("rows", List.of()), "failed", Map.of("rows", List.of())));
        when(service.getSettlementRequestSummary(any(AuthContext.class), eq("KR"), eq(null)))
                .thenReturn(Map.of("banner", Map.of("period", "2026.06"), "stats", List.of()));
        when(service.getSettlementHistory(any(AuthContext.class), eq("KR")))
                .thenReturn(Map.of("lastSettleDate", "-", "rows", List.of()));
        when(service.getSettlementDetail(any(AuthContext.class), eq("KR")))
                .thenReturn(Map.of("no", "-", "basicInfo", List.of()));
        when(service.getHqNotices(any(AuthContext.class), eq("KR")))
                .thenReturn(Map.of("rows", List.of(Map.of("no", "N-1"))));
        when(service.getNoticeSendSummary(any(AuthContext.class), eq("KR")))
                .thenReturn(Map.of("metrics", List.of()));
        when(service.getNoticeHistory(any(AuthContext.class), eq("KR")))
                .thenReturn(Map.of("metrics", List.of(), "rows", List.of()));
        when(service.getProfile(any(AuthContext.class), eq("KR")))
                .thenReturn(Map.of("code", "SP-00010", "statusItems", List.of()));
        when(service.getActivityLogs(any(AuthContext.class), eq("KR")))
                .thenReturn(Map.of("metrics", List.of(), "rows", List.of()));

        assertLeaderPage("/api/leader/partner-applications", "$.stats[0].labelKey", "partner.kpi.active");
        assertLeaderPage("/api/leader/partner-requests", "$.stats[0].labelKey", "partner.kpi.active");
        assertLeaderPage("/api/leader/merchant-applications", "$.stats[0].labelKey", "merchant.kpi.active");
        assertLeaderPage("/api/leader/merchant-requests", "$.stats[0].labelKey", "merchant.kpi.active");
        assertLeaderPage("/api/leader/partner-sales", "$.t1Rows.length()", 0);
        assertLeaderPage("/api/leader/merchants", "$.rows[0].merchantCode", "MER-00001");
        assertLeaderPage("/api/leader/merchant-sales", "$.t2Rows.length()", 0);
        assertLeaderPage("/api/leader/transactions", "$.all.rows.length()", 0);
        assertLeaderPage("/api/leader/settlements/request-summary", "$.banner.period", "2026.06");
        assertLeaderPage("/api/leader/settlements", "$.lastSettleDate", "-");
        assertLeaderPage("/api/leader/settlement-history", "$.lastSettleDate", "-");
        assertLeaderPage("/api/leader/settlements/detail", "$.no", "-");
        assertLeaderPage("/api/leader/hq-notices", "$.rows[0].no", "N-1");
        assertLeaderPage("/api/leader/notices/send-summary", "$.metrics.length()", 0);
        assertLeaderPage("/api/leader/notices", "$.rows.length()", 0);
        assertLeaderPage("/api/leader/notices/history", "$.rows.length()", 0);
        assertLeaderPage("/api/leader/profile", "$.code", "SP-00010");
        assertLeaderPage("/api/leader/activity-logs", "$.rows.length()", 0);
    }

    @Test
    void invalidLeaderHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/leader/dashboard")
                        .header("X-Leader-Id", "not-number")
                        .header("X-Country-Scopes", "KR")
                        .queryParam("period", "2026-06")
                        .queryParam("countryScope", "KR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("BAD_REQUEST")));
    }

    private void assertLeaderPage(String path, String jsonPath, Object expected) throws Exception {
        mockMvc.perform(get(path)
                        .header("X-Leader-Id", "10")
                        .header("X-Country-Scopes", "KR")
                        .queryParam("countryScope", "KR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(jsonPath, equalTo(expected)));
    }
}
