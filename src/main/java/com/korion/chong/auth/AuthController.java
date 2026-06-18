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

    @PostMapping("/signup-applications")
    public SignupApplicationResponse createSignupApplication(@Valid @RequestBody SignupApplicationRequest request) {
        return service.createSignupApplication(request);
    }
}
