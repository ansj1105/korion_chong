package com.korion.chong.partner;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.korion.chong.api.GlobalExceptionHandler;
import com.korion.chong.notice.NoticeService;
import com.korion.chong.settlement.SettlementService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PartnerControllerContractTest {
    private PartnerDashboardService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(PartnerDashboardService.class);
        SettlementService settlementService = Mockito.mock(SettlementService.class);
        NoticeService noticeService = Mockito.mock(NoticeService.class);
        PartnerController controller = new PartnerController(new PartnerAuthContextFactory(), service, settlementService, noticeService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void partnerPageEndpointsReturnFrontendContractShapes() throws Exception {
        when(service.getDashboard(any(PartnerAuthContext.class), eq("KR"), eq(null)))
                .thenReturn(Map.of("kpis", List.of(Map.of("id", "sub-merchant", "labelKey", "pdash.kpi.subMerchant", "value", "3"))));
        when(service.getMerchantApplications(any(PartnerAuthContext.class), eq("KR")))
                .thenReturn(Map.of("stats", List.of(Map.of("id", "active", "labelKey", "preq.kpi.active", "value", "1")), "rows", List.of()));
        when(service.getMerchantApplicationDetail(any(PartnerAuthContext.class), eq("KR")))
                .thenReturn(Map.of("code", "SP-00020", "statusItems", List.of()));
        when(service.getMerchants(any(PartnerAuthContext.class), eq("KR")))
                .thenReturn(Map.of("stats", List.of(), "rows", List.of(Map.of("merchantCode", "MER-00001"))));
        when(service.getMerchantSales(any(PartnerAuthContext.class), eq("KR"), eq(null)))
                .thenReturn(Map.of("stats", List.of(), "t1Rows", List.of(), "t2Rows", List.of()));
        when(service.getSettlementRequestSummary(any(PartnerAuthContext.class), eq("KR"), eq(null)))
                .thenReturn(Map.of("banner", Map.of("period", "2026.06"), "stats", List.of()));
        when(service.getSettlementHistory(any(PartnerAuthContext.class), eq("KR")))
                .thenReturn(Map.of("lastSettleDate", "-", "rows", List.of()));
        when(service.getSettlementDetail(any(PartnerAuthContext.class), eq("KR")))
                .thenReturn(Map.of("no", "-", "basicInfo", List.of()));
        when(service.getHqNotices(any(PartnerAuthContext.class), eq("KR")))
                .thenReturn(Map.of("rows", List.of(Map.of("no", "N-1"))));
        when(service.getNoticeSendSummary(any(PartnerAuthContext.class), eq("KR")))
                .thenReturn(Map.of("metrics", List.of()));
        when(service.getNoticeHistory(any(PartnerAuthContext.class), eq("KR")))
                .thenReturn(Map.of("metrics", List.of(), "rows", List.of()));
        when(service.getProfile(any(PartnerAuthContext.class), eq("KR")))
                .thenReturn(Map.of("code", "SP-00020", "statusItems", List.of()));
        when(service.getActivityLogs(any(PartnerAuthContext.class), eq("KR")))
                .thenReturn(Map.of("metrics", List.of(), "rows", List.of()));

        assertPartnerPage("/api/partner/dashboard", "$.kpis[0].id", "sub-merchant");
        assertPartnerPage("/api/partner/merchant-applications", "$.stats[0].labelKey", "preq.kpi.active");
        assertPartnerPage("/api/partner/merchant-applications/detail", "$.code", "SP-00020");
        assertPartnerPage("/api/partner/merchants", "$.rows[0].merchantCode", "MER-00001");
        assertPartnerPage("/api/partner/merchant-sales", "$.t2Rows.length()", 0);
        assertPartnerPage("/api/partner/settlements/request-summary", "$.banner.period", "2026.06");
        assertPartnerPage("/api/partner/settlements", "$.lastSettleDate", "-");
        assertPartnerPage("/api/partner/settlements/detail", "$.no", "-");
        assertPartnerPage("/api/partner/hq-notices", "$.rows[0].no", "N-1");
        assertPartnerPage("/api/partner/notices/send-summary", "$.metrics.length()", 0);
        assertPartnerPage("/api/partner/notices", "$.rows.length()", 0);
        assertPartnerPage("/api/partner/notices/history", "$.rows.length()", 0);
        assertPartnerPage("/api/partner/profile", "$.code", "SP-00020");
        assertPartnerPage("/api/partner/activity-logs", "$.rows.length()", 0);
    }

    @Test
    void invalidPartnerHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/partner/dashboard")
                        .header("X-Partner-Id", "not-number")
                        .header("X-Country-Scopes", "KR")
                        .queryParam("countryScope", "KR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("BAD_REQUEST")));
    }

    private void assertPartnerPage(String path, String jsonPath, Object expected) throws Exception {
        mockMvc.perform(get(path)
                        .header("X-Partner-Id", "20")
                        .header("X-Country-Scopes", "KR")
                        .queryParam("countryScope", "KR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(jsonPath, equalTo(expected)));
    }
}
