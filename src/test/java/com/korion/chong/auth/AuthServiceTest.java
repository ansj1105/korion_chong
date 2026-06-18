package com.korion.chong.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthServiceTest {
    private final FakeAuthRepository repository = new FakeAuthRepository();
    private final AuthService service = new AuthService(repository);

    @Test
    void signupApplicationCreatesSeparateApplicationWithoutGrantingUserAuthority() {
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
                "partner@example.com",
                "Partner Co",
                "Partner Owner",
                "010-0000-0000",
                "LEADER-KR",
                "KR",
                "Seoul",
                "Seoul",
                "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb",
                "regional sales plan",
                "req-1"
        ));

        assertThat(response.applicationId()).isEqualTo(123L);
        assertThat(response.status()).isEqualTo("SUBMITTED");
        assertThat(response.walletStored()).isTrue();
        assertThat(repository.created).isTrue();
        assertThat(repository.createdUser).isFalse();
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
        SignupApplicationRequest request = new SignupApplicationRequest(
                "MERCHANT",
                "merchant01",
                "merchant@example.com",
                "Merchant Store",
                "Merchant Owner",
                null,
                null,
                "KR",
                null,
                "Seoul",
                "not-tron",
                null,
                "req-2"
        );

        assertThatThrownBy(() -> service.createSignupApplication(request))
                .isInstanceOf(AuthValidationException.class)
                .hasMessageContaining("walletAddress");
    }

    private SignupApplicationRequest validRequest() {
        return new SignupApplicationRequest(
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
    }

    private static class FakeAuthRepository implements AuthRepository {
        boolean loginIdExists;
        boolean created;
        boolean createdUser;
        String activityTargetType;
        ReferralCodeValidationResponse referral;

        @Override
        public boolean loginIdExists(String loginId) {
            return loginIdExists;
        }

        @Override
        public boolean applicationEmailExists(String email) {
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
        public long createSignupApplication(SignupApplicationRequest request) {
            created = true;
            return 123L;
        }

        @Override
        public void recordActivity(String role, String actionType, String status, String targetType, Long targetId, String requestId) {
            activityTargetType = targetType;
        }
    }
}
