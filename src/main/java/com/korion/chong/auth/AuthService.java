package com.korion.chong.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final String TRON_ADDRESS_PATTERN = "^T[1-9A-HJ-NP-Za-km-z]{33}$";
    private static final Duration EMAIL_CODE_TTL = Duration.ofMinutes(10);

    private final AuthRepository repository;
    private final VerificationCodeGenerator codeGenerator;
    private final WalletSignatureVerifier walletSignatureVerifier;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AuthService(
            AuthRepository repository,
            VerificationCodeGenerator codeGenerator,
            WalletSignatureVerifier walletSignatureVerifier,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.walletSignatureVerifier = walletSignatureVerifier;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public AvailabilityResponse checkAvailability(String field, String value) {
        String normalizedField = normalizeField(field);
        boolean exists = switch (normalizedField) {
            case "loginId" -> repository.loginIdExists(value);
            case "email" -> repository.applicationEmailExists(value);
            case "telegram" -> repository.telegramExists(value);
            case "whatsapp" -> repository.whatsappExists(value);
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
    public EmailVerificationSendResponse sendEmailVerification(EmailVerificationSendRequest request) {
        if (repository.applicationEmailExists(request.email())) {
            throw new AuthValidationException("DUPLICATE_EMAIL", "email is already used by an open application");
        }
        String code = codeGenerator.generateSixDigitCode();
        Instant expiresAt = Instant.now(clock).plus(EMAIL_CODE_TTL);
        repository.createEmailVerification(request.email(), HashingSupport.sha256(code), expiresAt, request.requestId());
        repository.recordActivity("SYSTEM", "SIGNUP_EMAIL_VERIFICATION_SENT", "SUCCESS", "distributor_signup_email_verifications", null, request.requestId());
        return new EmailVerificationSendResponse(
                "EMAIL_VERIFICATION_SENT",
                "auth.emailVerification.sent",
                expiresAt
        );
    }

    @Transactional
    public EmailVerificationConfirmResponse confirmEmailVerification(EmailVerificationConfirmRequest request) {
        boolean verified = repository.confirmEmailVerification(
                request.email(),
                HashingSupport.sha256(request.code()),
                Instant.now(clock)
        );
        repository.recordActivity(
                "SYSTEM",
                "SIGNUP_EMAIL_VERIFICATION_CONFIRMED",
                verified ? "SUCCESS" : "FAILED",
                "distributor_signup_email_verifications",
                null,
                request.requestId()
        );
        if (!verified) {
            throw new AuthValidationException("INVALID_EMAIL_VERIFICATION_CODE", "email verification code is invalid or expired");
        }
        return new EmailVerificationConfirmResponse(true, "EMAIL_VERIFIED", "auth.emailVerification.verified");
    }

    @Transactional
    public WalletLinkVerifyResponse verifyWalletLink(WalletLinkVerifyRequest request) {
        if (!request.walletAddress().matches(TRON_ADDRESS_PATTERN)) {
            throw new AuthValidationException("INVALID_WALLET_ADDRESS", "walletAddress must be a TRON address");
        }
        if (repository.walletAddressExists(request.walletAddress())) {
            throw new AuthValidationException("DUPLICATE_WALLET_ADDRESS", "walletAddress is already verified or registered");
        }

        boolean verified = walletSignatureVerifier.verifyTronSignature(
                request.walletAddress(),
                request.nonce(),
                request.signature()
        );
        String status = verified ? "VERIFIED" : "FAILED";
        String errorCode = verified ? null : "WALLET_VERIFIER_NOT_CONFIGURED";
        String errorMessage = verified ? null : "Wallet signature verifier is not configured";
        repository.recordWalletVerification(
                request,
                HashingSupport.sha256(request.signature()),
                status,
                errorCode,
                errorMessage
        );
        repository.recordActivity(
                "SYSTEM",
                "SIGNUP_WALLET_VERIFICATION",
                verified ? "SUCCESS" : "FAILED",
                "distributor_signup_wallet_verifications",
                null,
                request.requestId()
        );
        if (!verified) {
            throw new AuthValidationException("WALLET_SIGNATURE_INVALID", "wallet signature could not be verified");
        }
        return new WalletLinkVerifyResponse(true, "VERIFIED", "WALLET_VERIFIED", "auth.wallet.verified");
    }

    @Transactional
    public SignupApplicationResponse createSignupApplication(SignupApplicationRequest request) {
        if (repository.loginIdExists(request.loginId())) {
            throw new AuthValidationException("DUPLICATE_LOGIN_ID", "loginId is already used");
        }
        if (repository.applicationEmailExists(request.email())) {
            throw new AuthValidationException("DUPLICATE_EMAIL", "email is already used by an open application");
        }
        if (!repository.emailVerified(request.email())) {
            throw new AuthValidationException("EMAIL_NOT_VERIFIED", "email verification is required before signup application");
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

        String passwordHash = passwordEncoder.encode(request.password());
        long applicationId = repository.createSignupApplication(request, passwordHash);
        repository.recordActivity("SYSTEM", "SIGNUP_APPLICATION_SUBMITTED", "REVIEWING", "distributor_signup_applications", applicationId, request.requestId());
        return new SignupApplicationResponse(
                applicationId,
                "REQUESTED",
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
            case "loginId", "email", "telegram", "whatsapp", "walletAddress" -> field;
            default -> throw new AuthValidationException("UNSUPPORTED_FIELD", "Unsupported availability field: " + field);
        };
    }
}
