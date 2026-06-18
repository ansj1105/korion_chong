package com.korion.chong.leader;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.korion.chong.api.GlobalExceptionHandler;
import com.korion.chong.notice.NoticeService;
import com.korion.chong.settlement.SettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SignupApprovalControllerContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private SignupApprovalService signupApprovalService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LeaderDashboardService dashboardService = Mockito.mock(LeaderDashboardService.class);
        signupApprovalService = Mockito.mock(SignupApprovalService.class);
        SettlementService settlementService = Mockito.mock(SettlementService.class);
        NoticeService noticeService = Mockito.mock(NoticeService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new LeaderController(new AuthContextFactory(), dashboardService, signupApprovalService, settlementService, noticeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void approveSignupApplicationReturnsActivatedRuntimeIds() throws Exception {
        Mockito.when(signupApprovalService.approve(any(AuthContext.class), eq(123L), any(SignupApprovalDecisionRequest.class)))
                .thenReturn(new SignupApprovalResponse(
                        123L,
                        "PARTNER",
                        "APPROVED",
                        500L,
                        600L,
                        null,
                        700L,
                        800L,
                        "SIGNUP_APPLICATION_APPROVED",
                        "approval.signup.approved"
                ));

        mockMvc.perform(post("/api/leader/signup-applications/{applicationId}/approve", 123L)
                        .header("X-Leader-Id", "10")
                        .header("X-Country-Scopes", "KR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupApprovalDecisionRequest("ok", "req-approve"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId", equalTo(123)))
                .andExpect(jsonPath("$.status", equalTo("APPROVED")))
                .andExpect(jsonPath("$.userId", equalTo(500)))
                .andExpect(jsonPath("$.partnerId", equalTo(600)))
                .andExpect(jsonPath("$.contractId", equalTo(700)))
                .andExpect(jsonPath("$.walletAddressId", equalTo(800)))
                .andExpect(jsonPath("$.resultCode", equalTo("SIGNUP_APPLICATION_APPROVED")));
    }

    @Test
    void rejectSignupApplicationReturnsRejectedStatus() throws Exception {
        Mockito.when(signupApprovalService.reject(any(AuthContext.class), eq(123L), any(SignupApprovalDecisionRequest.class)))
                .thenReturn(new SignupRejectionResponse(
                        123L,
                        "MERCHANT",
                        "REJECTED",
                        "SIGNUP_APPLICATION_REJECTED",
                        "approval.signup.rejected"
                ));

        mockMvc.perform(post("/api/leader/signup-applications/{applicationId}/reject", 123L)
                        .header("X-Leader-Id", "10")
                        .header("X-Country-Scopes", "KR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupApprovalDecisionRequest("bad docs", "req-reject"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId", equalTo(123)))
                .andExpect(jsonPath("$.applicantType", equalTo("MERCHANT")))
                .andExpect(jsonPath("$.status", equalTo("REJECTED")))
                .andExpect(jsonPath("$.resultCode", equalTo("SIGNUP_APPLICATION_REJECTED")));
    }
}
