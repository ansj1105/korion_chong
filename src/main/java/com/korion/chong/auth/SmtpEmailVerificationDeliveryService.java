package com.korion.chong.auth;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.Properties;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SmtpEmailVerificationDeliveryService implements EmailVerificationDeliveryService {
    private final SmtpMailProperties properties;

    public SmtpEmailVerificationDeliveryService(SmtpMailProperties properties) {
        this.properties = properties;
    }

    @Override
    public void sendVerificationCode(String email, String code, Instant expiresAt) {
        if (!properties.configured()) {
            throw new AuthValidationException("EMAIL_DELIVERY_NOT_CONFIGURED", "SMTP delivery is not configured");
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.getHost());
        sender.setPort(properties.getPort());
        if (StringUtils.hasText(properties.getUsername())) {
            sender.setUsername(properties.getUsername());
            sender.setPassword(properties.getPassword());
        }

        Properties mailProperties = sender.getJavaMailProperties();
        mailProperties.put("mail.smtp.auth", String.valueOf(StringUtils.hasText(properties.getUsername())));
        mailProperties.put("mail.smtp.starttls.enable", String.valueOf(properties.isStartTls()));
        mailProperties.put("mail.smtp.starttls.required", String.valueOf(properties.isStartTls()));

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(properties.getFrom());
            helper.setTo(email);
            helper.setSubject("[KORION] 이메일 인증번호");
            helper.setText(plainText(code), htmlText(code));
            sender.send(message);
        } catch (MailException | MessagingException exception) {
            throw new AuthValidationException("EMAIL_DELIVERY_FAILED", "email verification delivery failed");
        }
    }

    static String plainText(String code) {
        return """
                KORION 총판/가맹점 회원가입 이메일 인증번호입니다.

                인증번호: %s
                만료시간: 5분

                본인이 요청하지 않았다면 이 메일을 무시하세요.
                """.formatted(code);
    }

    static String htmlText(String code) {
        String safeCode = escapeHtml(code);
        String spacedCode = escapeHtml(String.join(" ", code.split("")));
        return """
                <!doctype html>
                <html lang="ko">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <meta http-equiv="x-apple-disable-message-reformatting">
                  <title>KORION 이메일 인증 코드</title>
                </head>
                <body style="margin:0;padding:0;background:#0b0818;">
                  <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:#0b0818;">
                    KORION 총판/가맹점 회원가입 이메일 인증번호 %s 입니다.
                  </div>
                  <table role="presentation" cellpadding="0" cellspacing="0" width="100%%" style="background:#0b0818;padding:38px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" cellpadding="0" cellspacing="0" width="600" style="width:600px;max-width:600px;background:#111827;border:1px solid #25304a;border-radius:18px;overflow:hidden;">
                          <tr>
                            <td style="padding:30px 32px;background:linear-gradient(135deg,#6d5cff,#8b5cf6);font-family:Arial,'Apple SD Gothic Neo','Noto Sans KR',sans-serif;color:#ffffff;">
                              <div style="font-size:14px;letter-spacing:2px;font-weight:700;">KORION PARTNERS</div>
                              <div style="margin-top:10px;font-size:27px;line-height:1.3;font-weight:800;">이메일 인증 코드</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:30px 32px 28px;font-family:Arial,'Apple SD Gothic Neo','Noto Sans KR',sans-serif;color:#e5e7eb;font-size:15px;line-height:1.7;">
                              <p style="margin:0 0 12px;font-weight:700;">안녕하세요,</p>
                              <p style="margin:0 0 22px;font-weight:700;">아래 인증 코드를 입력하여 이메일 인증을 완료해주세요.</p>
                              <div style="text-align:center;margin:20px 0 24px;">
                                <div style="display:inline-block;min-width:130px;padding:17px 22px;background:#0b1020;border:1px dashed #7c3aed;border-radius:12px;">
                                  <span style="font-size:28px;line-height:1;font-weight:800;letter-spacing:6px;color:#d8ccff;">%s</span>
                                </div>
                              </div>
                              <p style="margin:0 0 6px;color:#d9ddf0;">인증 코드는 <strong style="color:#ffffff;">5분간 유효</strong>합니다.</p>
                              <p style="margin:0 0 24px;color:#b8bfd4;">본인이 요청하지 않았다면 이 메일을 무시해 주세요.</p>
                              <a href="https://partners.korion.network" style="display:inline-block;padding:13px 22px;border-radius:9px;background:#6d5cff;color:#ffffff;text-decoration:none;font-size:14px;font-weight:800;">KORION PARTNERS 열기</a>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:18px 32px 26px;text-align:center;font-family:Arial,'Apple SD Gothic Neo','Noto Sans KR',sans-serif;color:#9ca3af;font-size:12px;">
                              © 2026 KORION. All rights reserved.
                            </td>
                          </tr>
                        </table>
                        <div style="margin-top:14px;font-family:Arial,'Apple SD Gothic Neo','Noto Sans KR',sans-serif;color:#5f6274;font-size:11px;">이 메일은 발신 전용입니다.</div>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(safeCode, spacedCode);
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
