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
import java.util.List;
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
        when(service.validateReferralCode(eq("NG-LEAD-001")))
                .thenReturn(new ReferralCodeValidationResponse(
                        true,
                        "NG-LEAD-001",
                        "COUNTRY_LEADER",
                        10L,
                        "NG",
                        "Lagos",
                        "VALID_CODE",
                        "auth.referral.valid"
                ));

        mockMvc.perform(get("/api/auth/referral-codes/{code}/validate", "NG-LEAD-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", equalTo(true)))
                .andExpect(jsonPath("$.code", equalTo("NG-LEAD-001")))
                .andExpect(jsonPath("$.codeType", equalTo("COUNTRY_LEADER")))
                .andExpect(jsonPath("$.ownerPartnerId", equalTo(10)))
                .andExpect(jsonPath("$.resultCode", equalTo("VALID_CODE")));
    }

    @Test
    void partnerReferralCodeValidateReturnsSalesPartnerContractShape() throws Exception {
        when(service.validateReferralCode(eq("NG-SP-004")))
                .thenReturn(new ReferralCodeValidationResponse(
                        true,
                        "NG-SP-004",
                        "SALES_PARTNER",
                        24L,
                        "NG",
                        "Lagos",
                        "VALID_CODE",
                        "auth.referral.valid"
                ));

        mockMvc.perform(get("/api/auth/referral-codes/{code}/validate", "NG-SP-004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", equalTo(true)))
                .andExpect(jsonPath("$.code", equalTo("NG-SP-004")))
                .andExpect(jsonPath("$.codeType", equalTo("SALES_PARTNER")))
                .andExpect(jsonPath("$.ownerPartnerId", equalTo(24)))
                .andExpect(jsonPath("$.resultCode", equalTo("VALID_CODE")));
    }

    @Test
    void signupOptionsReturnsCountryDropdownItems() throws Exception {
        when(service.signupOptions())
                .thenReturn(new SignupOptionsResponse(List.of(
                        new SignupCountryOption("NG", "Nigeria", "나이지리아", "🇳🇬"),
                        new SignupCountryOption("KR", "Korea (South)", "대한민국", "🇰🇷")
                )));

        mockMvc.perform(get("/api/auth/signup-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countries[0].code", equalTo("NG")))
                .andExpect(jsonPath("$.countries[0].nameEn", equalTo("Nigeria")))
                .andExpect(jsonPath("$.countries[0].nameKo", equalTo("나이지리아")))
                .andExpect(jsonPath("$.countries[0].flag", equalTo("🇳🇬")));
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
                "+821000000000",
                "@partner01",
                "+821000000000",
                null,
                "KR",
                "Seoul",
                null,
                null,
                null,
                "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb",
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
                        java.time.Instant.parse("2026-06-18T00:05:00Z")
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
    void telegramVerificationSendReturnsContractShape() throws Exception {
        when(service.sendTelegramVerification(any(TelegramVerificationSendRequest.class)))
                .thenReturn(new TelegramVerificationSendResponse(
                        "TELEGRAM_VERIFICATION_SENT",
                        "auth.telegramVerification.sent",
                        java.time.Instant.parse("2026-06-18T00:10:00Z")
                ));

        mockMvc.perform(post("/api/auth/telegram-verifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TelegramVerificationSendRequest(
                                "@partner01",
                                "req-telegram"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode", equalTo("TELEGRAM_VERIFICATION_SENT")))
                .andExpect(jsonPath("$.messageKey", equalTo("auth.telegramVerification.sent")));
    }

    @Test
    void telegramVerificationConfirmReturnsContractShape() throws Exception {
        when(service.confirmTelegramVerification(any(TelegramVerificationConfirmRequest.class)))
                .thenReturn(new TelegramVerificationConfirmResponse(
                        true,
                        "TELEGRAM_VERIFIED",
                        "auth.telegramVerification.verified"
                ));

        mockMvc.perform(post("/api/auth/telegram-verifications/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TelegramVerificationConfirmRequest(
                                "@partner01",
                                "123456",
                                "req-telegram"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified", equalTo(true)))
                .andExpect(jsonPath("$.resultCode", equalTo("TELEGRAM_VERIFIED")));
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

    @Test
    void walletAddressValidationReturnsContractShape() throws Exception {
        when(service.validateWalletAddress(any(WalletAddressValidateRequest.class)))
                .thenReturn(new WalletAddressValidateResponse(
                        true,
                        "EVM",
                        "VERIFIED",
                        "WALLET_ADDRESS_VERIFIED",
                        "auth.wallet.addressVerified"
                ));

        mockMvc.perform(post("/api/auth/wallet-addresses/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WalletAddressValidateRequest(
                                "PARTNER",
                                "partner@example.com",
                                "0x52908400098527886E0F7030069857D2E4169EE7",
                                "req-wallet-address"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified", equalTo(true)))
                .andExpect(jsonPath("$.walletNetwork", equalTo("EVM")))
                .andExpect(jsonPath("$.authStatus", equalTo("VERIFIED")))
                .andExpect(jsonPath("$.resultCode", equalTo("WALLET_ADDRESS_VERIFIED")));
    }

    @Test
    void loginReturnsContractShape() throws Exception {
        when(service.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse(
                        true,
                        100L,
                        "LEADER",
                        10L,
                        null,
                        List.of("KR"),
                        "/leader/dashboard",
                        false,
                        null,
                        "LOGIN_SUCCESS",
                        "auth.login.success"
                ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "leader01",
                                "password123",
                                "LEADER",
                                null,
                                "req-login"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated", equalTo(true)))
                .andExpect(jsonPath("$.role", equalTo("LEADER")))
                .andExpect(jsonPath("$.partnerId", equalTo(10)))
                .andExpect(jsonPath("$.countryScopes[0]", equalTo("KR")))
                .andExpect(jsonPath("$.redirectPath", equalTo("/leader/dashboard")))
                .andExpect(jsonPath("$.requiresTwoFactor", equalTo(false)))
                .andExpect(jsonPath("$.resultCode", equalTo("LOGIN_SUCCESS")));
    }
}
