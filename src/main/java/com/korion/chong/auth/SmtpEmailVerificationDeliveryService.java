package com.korion.chong.auth;

import java.time.Instant;
import java.util.Properties;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
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

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFrom());
        message.setTo(email);
        message.setSubject("[KORION] 이메일 인증번호");
        message.setText("""
                KORION 총판/가맹점 회원가입 이메일 인증번호입니다.

                인증번호: %s
                만료시간: 300초

                본인이 요청하지 않았다면 이 메일을 무시하세요.
                """.formatted(code));

        try {
            sender.send(message);
        } catch (MailException exception) {
            throw new AuthValidationException("EMAIL_DELIVERY_FAILED", "email verification delivery failed");
        }
    }
}
