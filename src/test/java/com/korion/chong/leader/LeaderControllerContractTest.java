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
    void invalidLeaderHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/leader/dashboard")
                        .header("X-Leader-Id", "not-number")
                        .header("X-Country-Scopes", "KR")
                        .queryParam("period", "2026-06")
                        .queryParam("countryScope", "KR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("BAD_REQUEST")));
    }
}
