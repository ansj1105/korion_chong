package com.korion.chong.merchant;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.korion.chong.api.GlobalExceptionHandler;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MerchantControllerContractTest {
    private MerchantDashboardService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(MerchantDashboardService.class);
        MerchantController controller = new MerchantController(new MerchantAuthContextFactory(), service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void merchantPageEndpointsReturnFrontendContractShapes() throws Exception {
        when(service.getDashboard(any(MerchantAuthContext.class), eq("KR"), eq(null)))
                .thenReturn(Map.of("kpis", List.of(Map.of("id", "today-sales", "labelKey", "mdash.kpi.todaySales", "value", "10 KORI", "accent", "cyan"))));
        when(service.getTransactions(any(MerchantAuthContext.class), eq("KR"), eq(null), eq("all")))
                .thenReturn(Map.of("stats", List.of(), "t1Rows", List.of(), "t2Rows", List.of()));
        when(service.getStore(any(MerchantAuthContext.class), eq("KR")))
                .thenReturn(Map.of("code", "MER-00030", "store", Map.of("merchantName", "Kori Cafe")));
        when(service.getProfile(any(MerchantAuthContext.class), eq("KR")))
                .thenReturn(Map.of("code", "MER-00030", "statusItems", List.of()));
        when(service.getHqNotices(any(MerchantAuthContext.class), eq("KR")))
                .thenReturn(Map.of("rows", List.of(Map.of("no", "N-1"))));
        when(service.getActivityLogs(any(MerchantAuthContext.class), eq("KR")))
                .thenReturn(Map.of("metrics", List.of(), "rows", List.of()));
        when(service.getSettlementHistory(any(MerchantAuthContext.class), eq("KR")))
                .thenReturn(Map.of("lastSettleDate", "-", "thisRequestAmount", "0 KORI", "rows", List.of()));
        when(service.getSettlementDetail(any(MerchantAuthContext.class), eq("KR")))
                .thenReturn(Map.of("no", "-", "status", "내역 없음", "basicInfo", List.of(), "amountSummary", List.of()));

        assertMerchantPage("/api/merchant/dashboard", "$.kpis[0].id", "today-sales");
        assertMerchantPage("/api/merchant/transactions", "$.t1Rows.length()", 0);
        assertMerchantPage("/api/merchant/payments", "$.t2Rows.length()", 0);
        assertMerchantPage("/api/merchant/store", "$.store.merchantName", "Kori Cafe");
        assertMerchantPage("/api/merchant/profile", "$.code", "MER-00030");
        assertMerchantPage("/api/merchant/hq-notices", "$.rows[0].no", "N-1");
        assertMerchantPage("/api/merchant/notices", "$.rows[0].no", "N-1");
        assertMerchantPage("/api/merchant/activity-logs", "$.rows.length()", 0);
        assertMerchantPage("/api/merchant/settlements", "$.rows.length()", 0);
        assertMerchantPage("/api/merchant/settlements/detail", "$.status", "내역 없음");
    }

    @Test
    void invalidMerchantHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/merchant/dashboard")
                        .header("X-Merchant-Id", "not-number")
                        .header("X-Country-Scopes", "KR")
                        .queryParam("countryScope", "KR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("BAD_REQUEST")));
    }

    private void assertMerchantPage(String path, String jsonPath, Object expected) throws Exception {
        mockMvc.perform(get(path)
                        .header("X-Merchant-Id", "30")
                        .header("X-Country-Scopes", "KR")
                        .queryParam("countryScope", "KR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(jsonPath, equalTo(expected)));
    }
}
