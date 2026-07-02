package com.korion.chong.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {
    private final FakeAuthRepository repository = new FakeAuthRepository();
    private final FakeEmailVerificationDeliveryService emailDeliveryService = new FakeEmailVerificationDeliveryService();
    private final FakeWalletSignatureVerifier walletSignatureVerifier = new FakeWalletSignatureVerifier();
    private final AuthService service = new AuthService(
            repository,
            new VerificationCodeGenerator() {
                @Override
                public String generateSixDigitCode() {
                    return "123456";
                }
            },
            emailDeliveryService,
            walletSignatureVerifier,
            new FakePasswordEncoder(),
            Clock.fixed(Instant.parse("2026-06-18T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void signupApplicationCreatesSeparateApplicationWithoutGrantingUserAuthority() {
        repository.emailVerified = true;
        repository.referral = new ReferralCodeValidationResponse(
                true,
                "NG-LEAD-001",
                "COUNTRY_LEADER",
                10L,
                "NG",
                "Lagos",
                "VALID_CODE",
                "auth.referral.valid"
        );

        SignupApplicationResponse response = service.createSignupApplication(new SignupApplicationRequest(
                "PARTNER",
                "partner01",
                "password123",
                "partner@example.com",
                "Partner Co",
                "Partner Owner",
                "010-0000-0000",
                "@partner01",
                "+821000000000",
                "ng-lead-001",
                "KR",
                "Seoul",
                "Seoul",
                null,
                null,
                "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb",
                "regional sales plan",
                null,
                "req-1"
        ));

        assertThat(response.applicationId()).isEqualTo(123L);
        assertThat(response.status()).isEqualTo("REQUESTED");
        assertThat(response.walletStored()).isTrue();
        assertThat(repository.created).isTrue();
        assertThat(repository.createdUser).isFalse();
        assertThat(repository.passwordHash).isEqualTo("hashed-password123");
        assertThat(repository.passwordHash).doesNotContain("Partner Co");
        assertThat(repository.submittedReferralCode).isEqualTo("NG-LEAD-001");
        assertThat(repository.activityTargetType).isEqualTo("distributor_signup_applications");
    }

    @Test
    void signupApplicationRejectsDuplicateTelegramAtSubmit() {
        repository.emailVerified = true;
        repository.telegramExists = true;

        assertThatThrownBy(() -> service.createSignupApplication(validRequest()))
                .isInstanceOf(AuthValidationException.class)
                .hasMessageContaining("telegram");
    }

    @Test
    void signupApplicationRejectsDuplicatePhoneAtSubmit() {
        repository.emailVerified = true;
        repository.phoneExists = true;

        assertThatThrownBy(() -> service.createSignupApplication(validRequest()))
                .isInstanceOf(AuthValidationException.class)
                .hasMessageContaining("phone");
    }

    @Test
    void signupApplicationRejectsDuplicateWhatsappAtSubmit() {
        repository.emailVerified = true;
        repository.whatsappExists = true;

        assertThatThrownBy(() -> service.createSignupApplication(validRequest()))
                .isInstanceOf(AuthValidationException.class)
                .hasMessageContaining("whatsapp");
    }

    @Test
    void validateReferralCodeRejectsInvalidFormatWithoutRepositoryLookup() {
        ReferralCodeValidationResponse response = service.validateReferralCode("NG-LEAD-0001");

        assertThat(response.valid()).isFalse();
        assertThat(response.code()).isEqualTo("NG-LEAD-0001");
        assertThat(response.resultCode()).isEqualTo("INVALID_CODE_FORMAT");
        assertThat(repository.referralLookupCount).isZero();
    }

    @Test
    void validateReferralCodeNormalizesBeforeRepositoryLookup() {
        repository.referral = new ReferralCodeValidationResponse(
                true,
                "NG-LEAD-001",
                "COUNTRY_LEADER",
                10L,
                "NG",
                "Lagos",
                "VALID_CODE",
                "auth.referral.valid"
        );

        ReferralCodeValidationResponse response = service.validateReferralCode(" ng-lead-001 ");

        assertThat(response.valid()).isTrue();
        assertThat(repository.lastReferralLookupCode).isEqualTo("NG-LEAD-001");
    }

    @Test
    void validateReferralCodeAcceptsPartnerCodeFormatAndLooksUpRepository() {
        repository.referral = new ReferralCodeValidationResponse(
                true,
                "NG-SP-004",
                "SALES_PARTNER",
                24L,
                "NG",
                "Lagos",
                "VALID_CODE",
                "auth.referral.valid"
        );

        ReferralCodeValidationResponse response = service.validateReferralCode(" ng-sp-004 ");

        assertThat(response.valid()).isTrue();
        assertThat(response.codeType()).isEqualTo("SALES_PARTNER");
        assertThat(response.ownerPartnerId()).isEqualTo(24L);
        assertThat(repository.lastReferralLookupCode).isEqualTo("NG-SP-004");
    }

    @Test
    void signupOptionsReturnsActiveCountryOptions() {
        SignupOptionsResponse response = service.signupOptions();

        assertThat(response.countries())
                .extracting(SignupCountryOption::code)
                .containsExactly("NG", "KR");
    }

    @Test
    void signupApplicationRejectsDuplicateLoginId() {
        repository.loginIdExists = true;

        assertThatThrownBy(() -> service.createSignupApplication(validRequest()))
                .isInstanceOf(AuthValidationException.class)
                .hasMessageContaining("loginId");
    }

    @Test
    void signupApplicationRejectsInvalidWalletAddress() {
        repository.emailVerified = true;
        SignupApplicationRequest request = new SignupApplicationRequest(
                "MERCHANT",
                "merchant01",
                "password123",
                "merchant@example.com",
                "Merchant Store",
                "Merchant Owner",
                "+821000000000",
                "@merchant01",
                "+821000000000",
                null,
                "KR",
                "Seoul",
                "Seoul",
                "Seoul address",
                "Retail",
                "not-tron",
                null,
                "business registration document",
                "req-2"
        );

        assertThatThrownBy(() -> service.createSignupApplication(request))
                .isInstanceOf(AuthValidationException.class)
                .hasMessageContaining("walletAddress");
    }

    @Test
    void walletAddressValidationAcceptsSupportedNetworks() {
        WalletAddressValidateResponse evmResponse = service.validateWalletAddress(new WalletAddressValidateRequest(
                "PARTNER",
                "partner@example.com",
                "0x52908400098527886E0F7030069857D2E4169EE7",
                "req-wallet-address"
        ));
        WalletAddressValidateResponse btcResponse = service.validateWalletAddress(new WalletAddressValidateRequest(
                "PARTNER",
                "partner@example.com",
                "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kygt080",
                "req-wallet-address-btc"
        ));

        assertThat(evmResponse.verified()).isTrue();
        assertThat(evmResponse.walletNetwork()).isEqualTo("EVM");
        assertThat(evmResponse.resultCode()).isEqualTo("WALLET_ADDRESS_VERIFIED");
        assertThat(btcResponse.verified()).isTrue();
        assertThat(btcResponse.walletNetwork()).isEqualTo("BTC");
    }

    @Test
    void walletAddressValidationRejectsUnsupportedAddress() {
        assertThatThrownBy(() -> service.validateWalletAddress(new WalletAddressValidateRequest(
                "PARTNER",
                "partner@example.com",
                "not-a-wallet",
                "req-wallet-address"
        )))
                .isInstanceOf(AuthValidationException.class)
                .hasMessageContaining("walletAddress");
    }

    @Test
    void signupApplicationRequiresVerifiedEmail() {
        assertThatThrownBy(() -> service.createSignupApplication(validRequest()))
                .isInstanceOf(AuthValidationException.class)
                .hasMessageContaining("email verification");
    }

    @Test
    void merchantSignupApplicationRequiresEvidenceNote() {
        repository.emailVerified = true;

        SignupApplicationRequest request = new SignupApplicationRequest(
                "MERCHANT",
                "merchant01",
                "password123",
                "merchant@example.com",
                "Merchant Store",
                "Merchant Owner",
                "+821000000000",
                "@merchant01",
                "+821000000000",
                null,
                "KR",
                "Seoul",
                "Seoul",
                "Seoul address",
                "Retail",
                "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb",
                null,
                null,
                "req-merchant"
        );

        assertThatThrownBy(() -> service.createSignupApplication(request))
                .isInstanceOf(AuthValidationException.class)
                .hasMessageContaining("evidenceNote");
    }

    @Test
    void sendEmailVerificationStoresOnlyCodeHash() {
        EmailVerificationSendResponse response = service.sendEmailVerification(
                new EmailVerificationSendRequest("partner@example.com", "req-email")
        );

        assertThat(response.resultCode()).isEqualTo("EMAIL_VERIFICATION_SENT");
        assertThat(response.expiresAt()).isEqualTo(Instant.parse("2026-06-18T00:05:00Z"));
        assertThat(repository.emailCodeHash).isEqualTo(HashingSupport.sha256("123456"));
        assertThat(repository.emailCodeHash).doesNotContain("123456");
        assertThat(repository.emailExpiresAt).isEqualTo(Instant.parse("2026-06-18T00:05:00Z"));
        assertThat(emailDeliveryService.email).isEqualTo("partner@example.com");
        assertThat(emailDeliveryService.code).isEqualTo("123456");
        assertThat(emailDeliveryService.expiresAt).isEqualTo(Instant.parse("2026-06-18T00:05:00Z"));
    }

    @Test
    void confirmEmailVerificationReturnsVerifiedResult() {
        repository.emailConfirmResult = true;

        EmailVerificationConfirmResponse response = service.confirmEmailVerification(
                new EmailVerificationConfirmRequest("partner@example.com", "123456", "req-email")
        );

        assertThat(response.verified()).isTrue();
    }

    @Test
    void walletVerificationStoresSignatureHashOnly() {
        walletSignatureVerifier.result = true;

        WalletLinkVerifyResponse response = service.verifyWalletLink(new WalletLinkVerifyRequest(
                "PARTNER",
                "partner@example.com",
                "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb",
                "nonce-1",
                "raw-signature",
                "req-wallet"
        ));

        assertThat(response.verified()).isTrue();
        assertThat(repository.walletStatus).isEqualTo("VERIFIED");
        assertThat(repository.signatureHash).isEqualTo(HashingSupport.sha256("raw-signature"));
        assertThat(repository.signatureHash).doesNotContain("raw-signature");
    }

    @Test
    void loginReturnsRoleScopedRedirectAndCountryScope() {
        repository.credential = new UserCredential(100L, "leader01", "hashed-password123", "ACTIVE");
        repository.roleContext = new LoginRoleContext("LEADER", 10L, null, List.of("KR"));

        LoginResponse response = service.login(new LoginRequest(
                "leader01",
                "password123",
                "LEADER",
                null,
                "req-login"
        ));

        assertThat(response.authenticated()).isTrue();
        assertThat(response.userId()).isEqualTo(100L);
        assertThat(response.partnerId()).isEqualTo(10L);
        assertThat(response.countryScopes()).containsExactly("KR");
        assertThat(response.redirectPath()).isEqualTo("/leader/dashboard");
        assertThat(response.requiresTwoFactor()).isFalse();
        assertThat(response.sessionExpiresAt()).isNull();
        assertThat(repository.activityStatus).isEqualTo("SUCCESS");
        assertThat(repository.activityTargetType).isEqualTo("users");
    }

    @Test
    void loginRejectsPasswordMismatch() {
        repository.credential = new UserCredential(100L, "leader01", "hashed-password123", "ACTIVE");

        assertThatThrownBy(() -> service.login(new LoginRequest(
                "leader01",
                "wrong-password",
                "LEADER",
                null,
                "req-login"
        )))
                .isInstanceOf(AuthValidationException.class)
                .hasMessageContaining("password");
        assertThat(repository.activityStatus).isEqualTo("FAILED");
    }

    @Test
    void loginRejectsUnapprovedRole() {
        repository.credential = new UserCredential(100L, "leader01", "hashed-password123", "ACTIVE");

        assertThatThrownBy(() -> service.login(new LoginRequest(
                "leader01",
                "password123",
                "PARTNER",
                null,
                "req-login"
        )))
                .isInstanceOf(AuthValidationException.class)
                .hasMessageContaining("role");
        assertThat(repository.activityStatus).isEqualTo("FAILED");
    }

    private SignupApplicationRequest validRequest() {
        return new SignupApplicationRequest(
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
    }

    private static class FakeAuthRepository implements AuthRepository {
        boolean loginIdExists;
        boolean telegramExists;
        boolean phoneExists;
        boolean whatsappExists;
        boolean created;
        boolean createdUser;
        boolean emailVerified;
        boolean emailConfirmResult;
        String emailCodeHash;
        Instant emailExpiresAt;
        String signatureHash;
        String passwordHash;
        String walletStatus;
        String activityTargetType;
        String activityStatus;
        String submittedReferralCode;
        String lastReferralLookupCode;
        int referralLookupCount;
        ReferralCodeValidationResponse referral;
        List<SignupCountryOption> countryOptions = List.of(
                new SignupCountryOption("NG", "Nigeria", "나이지리아", "🇳🇬"),
                new SignupCountryOption("KR", "Korea (South)", "대한민국", "🇰🇷")
        );
        UserCredential credential;
        LoginRoleContext roleContext;

        @Override
        public Optional<UserCredential> findUserCredential(String loginId) {
            return Optional.ofNullable(credential);
        }

        @Override
        public Optional<LoginRoleContext> findApprovedRoleContext(long userId, String role) {
            return Optional.ofNullable(roleContext)
                    .filter(context -> context.role().equals(role));
        }

        @Override
        public boolean loginIdExists(String loginId) {
            return loginIdExists;
        }

        @Override
        public boolean applicationEmailExists(String email) {
            return false;
        }

        @Override
        public boolean telegramExists(String telegram) {
            return telegramExists;
        }

        @Override
        public boolean phoneExists(String phone) {
            return phoneExists;
        }

        @Override
        public boolean whatsappExists(String whatsapp) {
            return whatsappExists;
        }

        @Override
        public boolean walletAddressExists(String walletAddress) {
            return false;
        }

        @Override
        public Optional<ReferralCodeValidationResponse> findReferralCode(String code) {
            referralLookupCount += 1;
            lastReferralLookupCode = code;
            return Optional.ofNullable(referral);
        }

        @Override
        public List<SignupCountryOption> findActiveSignupCountries() {
            return countryOptions;
        }

        @Override
        public void createEmailVerification(String email, String codeHash, Instant expiresAt, String requestId) {
            emailCodeHash = codeHash;
            emailExpiresAt = expiresAt;
        }

        @Override
        public boolean confirmEmailVerification(String email, String codeHash, Instant now) {
            return emailConfirmResult;
        }

        @Override
        public boolean emailVerified(String email) {
            return emailVerified;
        }

        @Override
        public void recordWalletVerification(WalletLinkVerifyRequest request, String signatureHash, String status, String errorCode, String errorMessage) {
            this.signatureHash = signatureHash;
            this.walletStatus = status;
        }

        @Override
        public long createSignupApplication(SignupApplicationRequest request, String passwordHash) {
            created = true;
            submittedReferralCode = request.referralCode();
            this.passwordHash = passwordHash;
            return 123L;
        }

        @Override
        public void recordActivity(String role, String actionType, String status, String targetType, Long targetId, String requestId) {
            activityTargetType = targetType;
            activityStatus = status;
        }
    }

    private static class FakeWalletSignatureVerifier implements WalletSignatureVerifier {
        boolean result;

        @Override
        public boolean verifyTronSignature(String address, String nonce, String signature) {
            return result;
        }
    }

    private static class FakeEmailVerificationDeliveryService implements EmailVerificationDeliveryService {
        String email;
        String code;
        Instant expiresAt;

        @Override
        public void sendVerificationCode(String email, String code, Instant expiresAt) {
            this.email = email;
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }

    private static class FakePasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(CharSequence rawPassword) {
            return "hashed-" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encode(rawPassword).equals(encodedPassword);
        }
    }
}
