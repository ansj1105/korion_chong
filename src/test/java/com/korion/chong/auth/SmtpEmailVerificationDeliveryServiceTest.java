package com.korion.chong.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SmtpEmailVerificationDeliveryServiceTest {
    @Test
    void htmlTextRendersVerificationCard() {
        String html = SmtpEmailVerificationDeliveryService.htmlText("399987");

        assertThat(html)
                .contains("KORION PARTNERS")
                .contains("이메일 인증 코드")
                .contains("3 9 9 9 8 7")
                .contains("5분간 유효")
                .contains("https://partners.korion.network");
    }

    @Test
    void htmlTextEscapesCode() {
        String html = SmtpEmailVerificationDeliveryService.htmlText("<123&>");

        assertThat(html).contains("&lt;123&amp;&gt;");
        assertThat(html).doesNotContain("<123&>");
    }

    @Test
    void plainTextKeepsReadableFallback() {
        String text = SmtpEmailVerificationDeliveryService.plainText("230209");

        assertThat(text)
                .contains("KORION 총판/가맹점 회원가입 이메일 인증번호입니다.")
                .contains("인증번호: 230209")
                .contains("만료시간: 5분");
    }
}
