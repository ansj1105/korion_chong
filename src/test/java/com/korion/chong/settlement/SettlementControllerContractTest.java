package com.korion.chong.settlement;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.korion.chong.api.GlobalExceptionHandler;
import com.korion.chong.leader.AuthContext;
import com.korion.chong.leader.AuthContextFactory;
import com.korion.chong.leader.LeaderController;
import com.korion.chong.leader.LeaderDashboardService;
import com.korion.chong.leader.SignupApprovalService;
import com.korion.chong.notice.NoticeService;
import com.korion.chong.partner.PartnerAuthContext;
import com.korion.chong.partner.PartnerAuthContextFactory;
import com.korion.chong.partner.PartnerController;
import com.korion.chong.partner.PartnerDashboardService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SettlementControllerContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private SettlementService settlementService;
    private MockMvc leaderMvc;
    private MockMvc partnerMvc;

    @BeforeEach
    void setUp() {
        settlementService = Mockito.mock(SettlementService.class);
        LeaderController leaderController = new LeaderController(
                new AuthContextFactory(),
                Mockito.mock(LeaderDashboardService.class),
                Mockito.mock(SignupApprovalService.class),
                settlementService,
                Mockito.mock(NoticeService.class)
        );
        PartnerController partnerController = new PartnerController(
                new PartnerAuthContextFactory(),
                Mockito.mock(PartnerDashboardService.class),
                settlementService,
                Mockito.mock(NoticeService.class)
        );
        leaderMvc = MockMvcBuilders.standaloneSetup(leaderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        partnerMvc = MockMvcBuilders.standaloneSetup(partnerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void leaderCanCreateSettlementRequest() throws Exception {
        Mockito.when(settlementService.createLeaderRequest(any(AuthContext.class), any(SettlementCreateRequest.class)))
                .thenReturn(response("REQUESTED", "SETTLEMENT_REQUEST_CREATED"));

        leaderMvc.perform(post("/api/leader/settlement-requests")
                        .header("X-Leader-Id", "10")
                        .header("X-Country-Scopes", "KR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("REQUESTED")))
                .andExpect(jsonPath("$.resultCode", equalTo("SETTLEMENT_REQUEST_CREATED")));
    }

    @Test
    void partnerCanCreateSettlementRequest() throws Exception {
        Mockito.when(settlementService.createPartnerRequest(any(PartnerAuthContext.class), any(SettlementCreateRequest.class)))
                .thenReturn(response("REQUESTED", "SETTLEMENT_REQUEST_CREATED"));

        partnerMvc.perform(post("/api/partner/settlements/requests")
                        .header("X-Partner-Id", "20")
                        .header("X-Country-Scopes", "KR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementRequestId", equalTo(101)))
                .andExpect(jsonPath("$.status", equalTo("REQUESTED")));
    }

    @Test
    void leaderCanApproveRejectAndMarkPaid() throws Exception {
        Mockito.when(settlementService.approve(any(AuthContext.class), eq(101L), any(SettlementActionRequest.class)))
                .thenReturn(response("APPROVED", "SETTLEMENT_REQUEST_APPROVED"));
        Mockito.when(settlementService.reject(any(AuthContext.class), eq(102L), any(SettlementActionRequest.class)))
                .thenReturn(response("REJECTED", "SETTLEMENT_REQUEST_REJECTED"));
        Mockito.when(settlementService.markPaid(any(AuthContext.class), eq(103L), any(SettlementActionRequest.class)))
                .thenReturn(response("PAID", "SETTLEMENT_REQUEST_PAID"));

        SettlementActionRequest action = new SettlementActionRequest(new BigDecimal("100.00"), "ok", "req-action");

        leaderMvc.perform(post("/api/leader/settlement-requests/{id}/approve", 101L)
                        .header("X-Leader-Id", "10")
                        .header("X-Country-Scopes", "KR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("APPROVED")));

        leaderMvc.perform(post("/api/leader/settlement-requests/{id}/reject", 102L)
                        .header("X-Leader-Id", "10")
                        .header("X-Country-Scopes", "KR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("REJECTED")));

        leaderMvc.perform(post("/api/leader/settlement-requests/{id}/mark-paid", 103L)
                        .header("X-Leader-Id", "10")
                        .header("X-Country-Scopes", "KR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("PAID")));
    }

    private SettlementCreateRequest createRequest() {
        return new SettlementCreateRequest(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 10),
                new BigDecimal("100.00"),
                900L,
                "monthly settlement",
                "req-settlement"
        );
    }

    private SettlementActionResponse response(String status, String resultCode) {
        return new SettlementActionResponse(
                101L,
                "SET-101",
                "PARTNER",
                20L,
                null,
                new BigDecimal("100.00"),
                "APPROVED".equals(status) || "PAID".equals(status) ? new BigDecimal("100.00") : BigDecimal.ZERO,
                BigDecimal.ZERO,
                status,
                resultCode,
                "settlement.request"
        );
    }
}
