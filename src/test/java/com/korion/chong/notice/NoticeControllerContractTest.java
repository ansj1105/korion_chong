package com.korion.chong.notice;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
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
import com.korion.chong.partner.PartnerAuthContext;
import com.korion.chong.partner.PartnerAuthContextFactory;
import com.korion.chong.partner.PartnerController;
import com.korion.chong.partner.PartnerDashboardService;
import com.korion.chong.settlement.SettlementService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class NoticeControllerContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private NoticeService noticeService;
    private MockMvc leaderMvc;
    private MockMvc partnerMvc;

    @BeforeEach
    void setUp() {
        noticeService = Mockito.mock(NoticeService.class);
        LeaderController leaderController = new LeaderController(
                new AuthContextFactory(),
                Mockito.mock(LeaderDashboardService.class),
                Mockito.mock(SignupApprovalService.class),
                Mockito.mock(SettlementService.class),
                noticeService
        );
        PartnerController partnerController = new PartnerController(
                new PartnerAuthContextFactory(),
                Mockito.mock(PartnerDashboardService.class),
                Mockito.mock(SettlementService.class),
                noticeService
        );
        leaderMvc = MockMvcBuilders.standaloneSetup(leaderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        partnerMvc = MockMvcBuilders.standaloneSetup(partnerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void leaderCanSendImmediateNotice() throws Exception {
        Mockito.when(noticeService.sendLeaderNotice(any(AuthContext.class), any(NoticeSendRequest.class)))
                .thenReturn(new NoticeSendResponse(
                        501L,
                        "SENT",
                        null,
                        Instant.parse("2026-06-19T00:00:00Z"),
                        2,
                        "NOTICE_SENT",
                        "notice.send.sent"
                ));

        leaderMvc.perform(post("/api/leader/notices/send")
                        .header("X-Leader-Id", "10")
                        .header("X-Country-Scopes", "KR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leaderRequest(null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noticeId", equalTo(501)))
                .andExpect(jsonPath("$.status", equalTo("SENT")))
                .andExpect(jsonPath("$.recipientCount", equalTo(2)))
                .andExpect(jsonPath("$.resultCode", equalTo("NOTICE_SENT")));
    }

    @Test
    void partnerCanScheduleMerchantNotice() throws Exception {
        Instant scheduledAt = Instant.parse("2026-06-20T00:00:00Z");
        Mockito.when(noticeService.sendPartnerNotice(any(PartnerAuthContext.class), any(NoticeSendRequest.class)))
                .thenReturn(new NoticeSendResponse(
                        601L,
                        "SCHEDULED",
                        scheduledAt,
                        null,
                        1,
                        "NOTICE_SEND_SCHEDULED",
                        "notice.send.scheduled"
                ));

        partnerMvc.perform(post("/api/partner/notices/outbox")
                        .header("X-Partner-Id", "20")
                        .header("X-Country-Scopes", "KR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partnerRequest(scheduledAt))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noticeId", equalTo(601)))
                .andExpect(jsonPath("$.status", equalTo("SCHEDULED")))
                .andExpect(jsonPath("$.recipientCount", equalTo(1)))
                .andExpect(jsonPath("$.resultCode", equalTo("NOTICE_SEND_SCHEDULED")));
    }

    private NoticeSendRequest leaderRequest(Instant scheduledAt) {
        return new NoticeSendRequest(
                "June notice",
                "OPERATIONS",
                "Notice body",
                "IN_APP",
                scheduledAt,
                List.of(
                        new NoticeRecipientRequest("PARTNER", 20L, null),
                        new NoticeRecipientRequest("MERCHANT", null, 30L)
                ),
                "req-notice-leader"
        );
    }

    private NoticeSendRequest partnerRequest(Instant scheduledAt) {
        return new NoticeSendRequest(
                "Merchant notice",
                "OPERATIONS",
                "Notice body",
                "IN_APP",
                scheduledAt,
                List.of(new NoticeRecipientRequest("MERCHANT", null, 30L)),
                "req-notice-partner"
        );
    }
}
