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
    void signupApplicationReturnsSubmittedWithoutSessionAuthority() throws Exception {
        when(service.createSignupApplication(any(SignupApplicationRequest.class)))
                .thenReturn(new SignupApplicationResponse(
                        123L,
                        "SUBMITTED",
                        "SIGNUP_APPLICATION_SUBMITTED",
                        "auth.signup.submitted",
                        false
                ));

        SignupApplicationRequest request = new SignupApplicationRequest(
                "PARTNER",
                "partner01",
                "partner@example.com",
                "Partner Co",
                "Partner Owner",
                null,
                null,
                "KR",
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
                .andExpect(jsonPath("$.status", equalTo("SUBMITTED")))
                .andExpect(jsonPath("$.walletStored", equalTo(false)));
    }
}
