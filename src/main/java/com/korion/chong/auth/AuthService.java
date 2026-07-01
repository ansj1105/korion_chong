package com.korion.chong.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final Pattern REFERRAL_CODE_PATTERN = Pattern.compile("^[A-Z]{2}-(LEAD|SP)-[0-9]{3}$");
    private static final Duration EMAIL_CODE_TTL = Duration.ofSeconds(300);
    private static final Duration TELEGRAM_CODE_TTL = Duration.ofMinutes(10);

    private final AuthRepository repository;
    private final VerificationCodeGenerator codeGenerator;
    private final EmailVerificationDeliveryService emailVerificationDeliveryService;
    private final WalletSignatureVerifier walletSignatureVerifier;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AuthService(
            AuthRepository repository,
            VerificationCodeGenerator codeGenerator,
            EmailVerificationDeliveryService emailVerificationDeliveryService,
            WalletSignatureVerifier walletSignatureVerifier,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.emailVerificationDeliveryService = emailVerificationDeliveryService;
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
            case "phone" -> repository.phoneExists(value);
            case "whatsapp" -> repository.whatsappExists(value);
            case "walletAddress" -> repository.walletAddressExists(value);
            default -> throw new AuthValidationException("UNSUPPORTED_FIELD", "Unsupported availability field: " + field);
        };
        return exists ? AvailabilityResponse.duplicate(normalizedField) : AvailabilityResponse.available(normalizedField);
    }

    public ReferralCodeValidationResponse validateReferralCode(String code) {
        String normalizedCode = normalizeReferralCode(code);
        if (!REFERRAL_CODE_PATTERN.matcher(normalizedCode).matches()) {
            return ReferralCodeValidationResponse.invalidFormat(normalizedCode);
        }
        return repository.findReferralCode(normalizedCode)
                .orElseGet(() -> ReferralCodeValidationResponse.invalid(normalizedCode));
    }

    public SignupOptionsResponse signupOptions() {
        List<SignupCountryOption> countries = repository.findActiveSignupCountries();
        return new SignupOptionsResponse(countries);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Optional<UserCredential> credential = repository.findUserCredential(request.loginId());
        if (credential.isEmpty() || !passwordEncoder.matches(request.password(), credential.get().passwordHash())) {
            credential.ifPresent(user -> repository.recordActivity(
                    request.role(),
                    "LOGIN",
                    "FAILED",
                    "users",
                    user.userId(),
                    request.requestId()
            ));
            throw new AuthValidationException("INVALID_CREDENTIALS", "loginId or password is invalid");
        }

        UserCredential user = credential.get();
        if (!"ACTIVE".equals(user.status())) {
            repository.recordActivity(request.role(), "LOGIN", "FAILED", "users", user.userId(), request.requestId());
            throw new AuthValidationException("ACCOUNT_INACTIVE", "account is not active");
        }

        LoginRoleContext context = repository.findApprovedRoleContext(user.userId(), request.role())
                .orElseThrow(() -> {
                    repository.recordActivity(request.role(), "LOGIN", "FAILED", "users", user.userId(), request.requestId());
                    return new AuthValidationException("ROLE_NOT_APPROVED", "selected role is not approved for this account");
                });

        repository.recordActivity(request.role(), "LOGIN", "SUCCESS", "users", user.userId(), request.requestId());
        return new LoginResponse(
                true,
                user.userId(),
                context.role(),
                context.partnerId(),
                context.merchantId(),
                context.countryScopes(),
                redirectPath(context.role()),
                false,
                null,
                "LOGIN_SUCCESS",
                "auth.login.success"
        );
    }

    @Transactional
    public EmailVerificationSendResponse sendEmailVerification(EmailVerificationSendRequest request) {
        if (repository.applicationEmailExists(request.email())) {
            throw new AuthValidationException("DUPLICATE_EMAIL", "email is already used by an open application");
        }
        String code = codeGenerator.generateSixDigitCode();
        Instant expiresAt = Instant.now(clock).plus(EMAIL_CODE_TTL);
        repository.createEmailVerification(request.email(), HashingSupport.sha256(code), expiresAt, request.requestId());
        emailVerificationDeliveryService.sendVerificationCode(request.email(), code, expiresAt);
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
    public TelegramVerificationSendResponse sendTelegramVerification(TelegramVerificationSendRequest request) {
        if (repository.telegramExists(request.telegram())) {
            throw new AuthValidationException("DUPLICATE_TELEGRAM", "telegram is already used by an open application");
        }
        String code = codeGenerator.generateSixDigitCode();
        Instant expiresAt = Instant.now(clock).plus(TELEGRAM_CODE_TTL);
        repository.createTelegramVerification(request.telegram(), HashingSupport.sha256(code), expiresAt, request.requestId());
        repository.recordActivity("SYSTEM", "SIGNUP_TELEGRAM_VERIFICATION_SENT", "SUCCESS", "distributor_signup_telegram_verifications", null, request.requestId());
        return new TelegramVerificationSendResponse(
                "TELEGRAM_VERIFICATION_SENT",
                "auth.telegramVerification.sent",
                expiresAt
        );
    }

    @Transactional
    public TelegramVerificationConfirmResponse confirmTelegramVerification(TelegramVerificationConfirmRequest request) {
        boolean verified = repository.confirmTelegramVerification(
                request.telegram(),
                HashingSupport.sha256(request.code()),
                Instant.now(clock)
        );
        repository.recordActivity(
                "SYSTEM",
                "SIGNUP_TELEGRAM_VERIFICATION_CONFIRMED",
                verified ? "SUCCESS" : "FAILED",
                "distributor_signup_telegram_verifications",
                null,
                request.requestId()
        );
        if (!verified) {
            throw new AuthValidationException("INVALID_TELEGRAM_VERIFICATION_CODE", "telegram verification code is invalid or expired");
        }
        return new TelegramVerificationConfirmResponse(true, "TELEGRAM_VERIFIED", "auth.telegramVerification.verified");
    }

    @Transactional
    public WalletLinkVerifyResponse verifyWalletLink(WalletLinkVerifyRequest request) {
        if (!WalletAddressSupport.isTronAddress(request.walletAddress())) {
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
    public WalletAddressValidateResponse validateWalletAddress(WalletAddressValidateRequest request) {
        String walletNetwork = WalletAddressSupport.detectNetwork(request.walletAddress())
                .orElseThrow(() -> new AuthValidationException(
                        "INVALID_WALLET_ADDRESS",
                        "walletAddress must be a supported KORION wallet address"
                ));
        if (repository.walletAddressExists(request.walletAddress())) {
            throw new AuthValidationException("DUPLICATE_WALLET_ADDRESS", "walletAddress is already verified or registered");
        }
        repository.recordActivity(
                "SYSTEM",
                "SIGNUP_WALLET_ADDRESS_VALIDATED",
                "SUCCESS",
                "distributor_signup_wallet_verifications",
                null,
                request.requestId()
        );
        return new WalletAddressValidateResponse(
                true,
                walletNetwork,
                "VERIFIED",
                "WALLET_ADDRESS_VERIFIED",
                "auth.wallet.addressVerified"
        );
    }

    @Transactional
    public SignupApplicationResponse createSignupApplication(SignupApplicationRequest request) {
        validateRequiredSignupFields(request);
        if (repository.loginIdExists(request.loginId())) {
            throw new AuthValidationException("DUPLICATE_LOGIN_ID", "loginId is already used");
        }
        if (repository.applicationEmailExists(request.email())) {
            throw new AuthValidationException("DUPLICATE_EMAIL", "email is already used by an open application");
        }
        if (!repository.emailVerified(request.email())) {
            throw new AuthValidationException("EMAIL_NOT_VERIFIED", "email verification is required before signup application");
        }
        if (!repository.telegramVerified(request.telegram())) {
            throw new AuthValidationException("TELEGRAM_NOT_VERIFIED", "telegram verification is required before signup application");
        }
        if (request.walletAddress() != null && !request.walletAddress().isBlank()) {
            WalletAddressSupport.detectNetwork(request.walletAddress())
                    .orElseThrow(() -> new AuthValidationException(
                            "INVALID_WALLET_ADDRESS",
                            "walletAddress must be a supported KORION wallet address"
                    ));
            if (repository.walletAddressExists(request.walletAddress())) {
                throw new AuthValidationException("DUPLICATE_WALLET_ADDRESS", "walletAddress is already verified or registered");
            }
        }
        String normalizedReferralCode = normalizeReferralCode(request.referralCode());
        SignupApplicationRequest normalizedRequest = normalizedReferralCode.isBlank()
                ? request
                : withReferralCode(request, normalizedReferralCode);
        if (!normalizedReferralCode.isBlank()
                && !validateReferralCode(normalizedReferralCode).valid()) {
            throw new AuthValidationException("INVALID_REFERRAL_CODE", "referralCode is not active");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        long applicationId = repository.createSignupApplication(normalizedRequest, passwordHash);
        repository.recordActivity("SYSTEM", "SIGNUP_APPLICATION_SUBMITTED", "REVIEWING", "distributor_signup_applications", applicationId, normalizedRequest.requestId());
        return new SignupApplicationResponse(
                applicationId,
                "REQUESTED",
                "SIGNUP_APPLICATION_SUBMITTED",
                "auth.signup.submitted",
                normalizedRequest.walletAddress() != null && !normalizedRequest.walletAddress().isBlank()
        );
    }

    private String normalizeReferralCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private SignupApplicationRequest withReferralCode(SignupApplicationRequest request, String referralCode) {
        return new SignupApplicationRequest(
                request.applicantType(),
                request.loginId(),
                request.password(),
                request.email(),
                request.companyName(),
                request.contactName(),
                request.phone(),
                request.telegram(),
                request.whatsapp(),
                referralCode,
                request.country(),
                request.region(),
                request.city(),
                request.address(),
                request.businessType(),
                request.walletAddress(),
                request.integrationPlan(),
                request.evidenceNote(),
                request.requestId()
        );
    }

    private void validateRequiredSignupFields(SignupApplicationRequest request) {
        requireText(request.telegram(), "telegram");
        requireText(request.whatsapp(), "whatsapp");
        requireText(request.country(), "country");
        requireText(request.region(), "region");
        requireText(request.walletAddress(), "walletAddress");
        if ("MERCHANT".equals(request.applicantType())) {
            requireText(request.address(), "address");
            requireText(request.businessType(), "businessType");
            requireText(request.evidenceNote(), "evidenceNote");
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new AuthValidationException("REQUIRED_FIELD_MISSING", field + " is required");
        }
    }

    private String normalizeField(String field) {
        if (field == null) {
            throw new AuthValidationException("FIELD_REQUIRED", "field is required");
        }
        return switch (field) {
            case "loginId", "email", "telegram", "phone", "whatsapp", "walletAddress" -> field;
            default -> throw new AuthValidationException("UNSUPPORTED_FIELD", "Unsupported availability field: " + field);
        };
    }

    private String redirectPath(String role) {
        return switch (role) {
            case "LEADER" -> "/leader/dashboard";
            case "PARTNER" -> "/partner/dashboard";
            case "MERCHANT" -> "/merchant/dashboard";
            default -> throw new AuthValidationException("UNSUPPORTED_ROLE", "Unsupported login role: " + role);
        };
    }
}
