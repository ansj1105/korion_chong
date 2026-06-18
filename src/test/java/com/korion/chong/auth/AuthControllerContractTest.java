package com.korion.chong.auth;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.korion.chong.api.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private AuthService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void availabilityReturnsStandardResultCode() throws Exception {
        when(service.checkAvailability(eq("loginId"), eq("partner01")))
                .thenReturn(AvailabilityResponse.available("loginId"));

        mockMvc.perform(get("/api/auth/availability")
                        .queryParam("field", "loginId")
                        .queryParam("value", "partner01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available", equalTo(true)))
                .andExpect(jsonPath("$.resultCode", equalTo("AVAILABLE")));
    }

    @Test
    void referralCodeValidateReturnsContractShape() throws Exception {
        when(service.validateReferralCode(eq("LEADER-KR")))
                .thenReturn(new ReferralCodeValidationResponse(
                        true,
                        "LEADER-KR",
                        "COUNTRY_LEADER",
                        10L,
                        "KR",
                        "Seoul",
                        "VALID_CODE",
                        "auth.referral.valid"
                ));

        mockMvc.perform(get("/api/auth/referral-codes/{code}/validate", "LEADER-KR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", equalTo(true)))
                .andExpect(jsonPath("$.code", equalTo("LEADER-KR")))
                .andExpect(jsonPath("$.codeType", equalTo("COUNTRY_LEADER")))
                .andExpect(jsonPath("$.ownerPartnerId", equalTo(10)))
                .andExpect(jsonPath("$.resultCode", equalTo("VALID_CODE")));
    }

    @Test
    void authValidationErrorReturnsServiceCode() throws Exception {
        when(service.checkAvailability(eq("unknown"), eq("value")))
                .thenThrow(new AuthValidationException(
                        "UNSUPPORTED_FIELD",
                        "Unsupported availability field: unknown"
                ));

        mockMvc.perform(get("/api/auth/availability")
                        .queryParam("field", "unknown")
                        .queryParam("value", "value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("UNSUPPORTED_FIELD")));
    }

    @Test
    void signupApplicationReturnsSubmittedWithoutSessionAuthority() throws Exception {
        when(service.createSignupApplication(any(SignupApplicationRequest.class)))
                .thenReturn(new SignupApplicationResponse(
                        123L,
                        "REQUESTED",
                        "SIGNUP_APPLICATION_SUBMITTED",
                        "auth.signup.submitted",
                        false
                ));

        SignupApplicationRequest request = new SignupApplicationRequest(
                "PARTNER",
                "partner01",
                "password123",
                "partner@example.com",
                "Partner Co",
                "Partner Owner",
                null,
                null,
                null,
                null,
                "KR",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "req-1"
        );

        mockMvc.perform(post("/api/auth/signup-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId", equalTo(123)))
                .andExpect(jsonPath("$.status", equalTo("REQUESTED")))
                .andExpect(jsonPath("$.walletStored", equalTo(false)));
    }

    @Test
    void emailVerificationSendReturnsContractShape() throws Exception {
        when(service.sendEmailVerification(any(EmailVerificationSendRequest.class)))
                .thenReturn(new EmailVerificationSendResponse(
                        "EMAIL_VERIFICATION_SENT",
                        "auth.emailVerification.sent",
                        java.time.Instant.parse("2026-06-18T00:10:00Z")
                ));

        mockMvc.perform(post("/api/auth/email-verifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmailVerificationSendRequest(
                                "partner@example.com",
                                "req-email"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode", equalTo("EMAIL_VERIFICATION_SENT")))
                .andExpect(jsonPath("$.messageKey", equalTo("auth.emailVerification.sent")));
    }

    @Test
    void emailVerificationConfirmReturnsContractShape() throws Exception {
        when(service.confirmEmailVerification(any(EmailVerificationConfirmRequest.class)))
                .thenReturn(new EmailVerificationConfirmResponse(
                        true,
                        "EMAIL_VERIFIED",
                        "auth.emailVerification.verified"
                ));

        mockMvc.perform(post("/api/auth/email-verifications/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmailVerificationConfirmRequest(
                                "partner@example.com",
                                "123456",
                                "req-email"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified", equalTo(true)))
                .andExpect(jsonPath("$.resultCode", equalTo("EMAIL_VERIFIED")));
    }

    @Test
    void walletVerificationReturnsContractShape() throws Exception {
        when(service.verifyWalletLink(any(WalletLinkVerifyRequest.class)))
                .thenReturn(new WalletLinkVerifyResponse(
                        true,
                        "VERIFIED",
                        "WALLET_VERIFIED",
                        "auth.wallet.verified"
                ));

        mockMvc.perform(post("/api/auth/wallet-links/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WalletLinkVerifyRequest(
                                "PARTNER",
                                "partner@example.com",
                                "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb",
                                "nonce-1",
                                "signature",
                                "req-wallet"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified", equalTo(true)))
                .andExpect(jsonPath("$.authStatus", equalTo("VERIFIED")));
    }
}
