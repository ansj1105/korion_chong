package com.korion.chong.auth;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @GetMapping("/availability")
    public AvailabilityResponse availability(@RequestParam String field, @RequestParam String value) {
        return service.checkAvailability(field, value);
    }

    @GetMapping("/referral-codes/{code}/validate")
    public ReferralCodeValidationResponse validateReferralCode(@PathVariable String code) {
        return service.validateReferralCode(code);
    }

    @GetMapping("/signup-options")
    public SignupOptionsResponse signupOptions() {
        return service.signupOptions();
    }

    @PostMapping("/email-verifications/send")
    public EmailVerificationSendResponse sendEmailVerification(@Valid @RequestBody EmailVerificationSendRequest request) {
        return service.sendEmailVerification(request);
    }

    @PostMapping("/email-verifications/confirm")
    public EmailVerificationConfirmResponse confirmEmailVerification(@Valid @RequestBody EmailVerificationConfirmRequest request) {
        return service.confirmEmailVerification(request);
    }

    @PostMapping("/wallet-links/verify")
    public WalletLinkVerifyResponse verifyWalletLink(@Valid @RequestBody WalletLinkVerifyRequest request) {
        return service.verifyWalletLink(request);
    }

    @PostMapping("/wallet-addresses/validate")
    public WalletAddressValidateResponse validateWalletAddress(@Valid @RequestBody WalletAddressValidateRequest request) {
        return service.validateWalletAddress(request);
    }

    @PostMapping("/signup-applications")
    public SignupApplicationResponse createSignupApplication(@Valid @RequestBody SignupApplicationRequest request) {
        return service.createSignupApplication(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }
}
