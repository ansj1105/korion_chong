package com.korion.chong.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final String TRON_ADDRESS_PATTERN = "^T[1-9A-HJ-NP-Za-km-z]{33}$";

    private final AuthRepository repository;

    public AuthService(AuthRepository repository) {
        this.repository = repository;
    }

    public AvailabilityResponse checkAvailability(String field, String value) {
        String normalizedField = normalizeField(field);
        boolean exists = switch (normalizedField) {
            case "loginId" -> repository.loginIdExists(value);
            case "email" -> repository.applicationEmailExists(value);
            case "walletAddress" -> repository.walletAddressExists(value);
            default -> throw new AuthValidationException("UNSUPPORTED_FIELD", "Unsupported availability field: " + field);
        };
        return exists ? AvailabilityResponse.duplicate(normalizedField) : AvailabilityResponse.available(normalizedField);
    }

    public ReferralCodeValidationResponse validateReferralCode(String code) {
        return repository.findReferralCode(code)
                .orElseGet(() -> ReferralCodeValidationResponse.invalid(code));
    }

    @Transactional
    public SignupApplicationResponse createSignupApplication(SignupApplicationRequest request) {
        if (repository.loginIdExists(request.loginId())) {
            throw new AuthValidationException("DUPLICATE_LOGIN_ID", "loginId is already used");
        }
        if (repository.applicationEmailExists(request.email())) {
            throw new AuthValidationException("DUPLICATE_EMAIL", "email is already used by an open application");
        }
        if (request.walletAddress() != null && !request.walletAddress().isBlank()) {
            if (!request.walletAddress().matches(TRON_ADDRESS_PATTERN)) {
                throw new AuthValidationException("INVALID_WALLET_ADDRESS", "walletAddress must be a TRON address");
            }
            if (repository.walletAddressExists(request.walletAddress())) {
                throw new AuthValidationException("DUPLICATE_WALLET_ADDRESS", "walletAddress is already verified or registered");
            }
        }
        if (request.referralCode() != null && !request.referralCode().isBlank()
                && !validateReferralCode(request.referralCode()).valid()) {
            throw new AuthValidationException("INVALID_REFERRAL_CODE", "referralCode is not active");
        }

        long applicationId = repository.createSignupApplication(request);
        repository.recordActivity("SYSTEM", "SIGNUP_APPLICATION_SUBMITTED", "REVIEWING", "distributor_signup_applications", applicationId, request.requestId());
        return new SignupApplicationResponse(
                applicationId,
                "SUBMITTED",
                "SIGNUP_APPLICATION_SUBMITTED",
                "auth.signup.submitted",
                request.walletAddress() != null && !request.walletAddress().isBlank()
        );
    }

    private String normalizeField(String field) {
        if (field == null) {
            throw new AuthValidationException("FIELD_REQUIRED", "field is required");
        }
        return switch (field) {
            case "loginId", "email", "walletAddress" -> field;
            default -> throw new AuthValidationException("UNSUPPORTED_FIELD", "Unsupported availability field: " + field);
        };
    }
}
