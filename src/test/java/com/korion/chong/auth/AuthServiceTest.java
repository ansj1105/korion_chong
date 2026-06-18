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
    private final FakeWalletSignatureVerifier walletSignatureVerifier = new FakeWalletSignatureVerifier();
    private final AuthService service = new AuthService(
            repository,
            new VerificationCodeGenerator() {
                @Override
                public String generateSixDigitCode() {
                    return "123456";
                }
            },
            walletSignatureVerifier,
            new FakePasswordEncoder(),
            Clock.fixed(Instant.parse("2026-06-18T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void signupApplicationCreatesSeparateApplicationWithoutGrantingUserAuthority() {
        repository.emailVerified = true;
        repository.referral = new ReferralCodeValidationResponse(
                true,
                "LEADER-KR",
                "COUNTRY_LEADER",
                10L,
                "KR",
                "Seoul",
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
                "LEADER-KR",
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
        assertThat(repository.activityTargetType).isEqualTo("distributor_signup_applications");
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
                null,
                null,
                null,
                null,
                "KR",
                null,
                "Seoul",
                "Seoul address",
                "Retail",
                "not-tron",
                null,
                null,
                "req-2"
        );

        assertThatThrownBy(() -> service.createSignupApplication(request))
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
    void sendEmailVerificationStoresOnlyCodeHash() {
        EmailVerificationSendResponse response = service.sendEmailVerification(
                new EmailVerificationSendRequest("partner@example.com", "req-email")
        );

        assertThat(response.resultCode()).isEqualTo("EMAIL_VERIFICATION_SENT");
        assertThat(repository.emailCodeHash).isEqualTo(HashingSupport.sha256("123456"));
        assertThat(repository.emailCodeHash).doesNotContain("123456");
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
    }

    private static class FakeAuthRepository implements AuthRepository {
        boolean loginIdExists;
        boolean created;
        boolean createdUser;
        boolean emailVerified;
        boolean emailConfirmResult;
        String emailCodeHash;
        String signatureHash;
        String passwordHash;
        String walletStatus;
        String activityTargetType;
        String activityStatus;
        ReferralCodeValidationResponse referral;
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
            return false;
        }

        @Override
        public boolean whatsappExists(String whatsapp) {
            return false;
        }

        @Override
        public boolean walletAddressExists(String walletAddress) {
            return false;
        }

        @Override
        public Optional<ReferralCodeValidationResponse> findReferralCode(String code) {
            return Optional.ofNullable(referral);
        }

        @Override
        public void createEmailVerification(String email, String codeHash, Instant expiresAt, String requestId) {
            emailCodeHash = codeHash;
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
