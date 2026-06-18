package com.korion.chong.auth;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuthRepository implements AuthRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcAuthRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean loginIdExists(String loginId) {
        return count("""
                SELECT COUNT(*)
                  FROM users
                 WHERE lower(login_id) = lower(:loginId)
                """, Map.of("loginId", loginId)) > 0
                || count("""
                SELECT COUNT(*)
                  FROM distributor_signup_applications
                 WHERE lower(login_id) = lower(:loginId)
                   AND status IN ('REQUESTED', 'REVIEWING', 'NEED_MORE_INFO', 'HOLD', 'APPROVED')
                """, Map.of("loginId", loginId)) > 0;
    }

    @Override
    public boolean applicationEmailExists(String email) {
        return count("""
                SELECT COUNT(*)
                  FROM distributor_signup_applications
                 WHERE lower(email) = lower(:email)
                   AND status IN ('REQUESTED', 'REVIEWING', 'NEED_MORE_INFO', 'HOLD', 'APPROVED')
                """, Map.of("email", email)) > 0;
    }

    @Override
    public boolean telegramExists(String telegram) {
        return count("""
                SELECT COUNT(*)
                  FROM distributor_signup_applications
                 WHERE lower(telegram) = lower(:telegram)
                   AND status IN ('REQUESTED', 'REVIEWING', 'NEED_MORE_INFO', 'HOLD', 'APPROVED')
                """, Map.of("telegram", telegram)) > 0;
    }

    @Override
    public boolean whatsappExists(String whatsapp) {
        return count("""
                SELECT COUNT(*)
                  FROM distributor_signup_applications
                 WHERE lower(whatsapp) = lower(:whatsapp)
                   AND status IN ('REQUESTED', 'REVIEWING', 'NEED_MORE_INFO', 'HOLD', 'APPROVED')
                """, Map.of("whatsapp", whatsapp)) > 0;
    }

    @Override
    public boolean walletAddressExists(String walletAddress) {
        Map<String, String> params = Map.of("walletAddress", walletAddress);
        return count("""
                SELECT COUNT(*)
                  FROM distributor_wallet_addresses
                 WHERE network = 'TRON'
                   AND lower(address) = lower(:walletAddress)
                   AND auth_status IN ('UNVERIFIED', 'VERIFIED')
                   AND revoked_at IS NULL
                """, params) > 0
                || count("""
                SELECT COUNT(*)
                  FROM distributor_signup_applications
                 WHERE wallet_network = 'TRON'
                   AND lower(wallet_address) = lower(:walletAddress)
                   AND status IN ('REQUESTED', 'REVIEWING', 'NEED_MORE_INFO', 'HOLD', 'APPROVED')
                """, params) > 0;
    }

    @Override
    public Optional<ReferralCodeValidationResponse> findReferralCode(String code) {
        List<ReferralCodeValidationResponse> rows = jdbcTemplate.query("""
                SELECT id, code, code_type, owner_partner_id, country, city
                  FROM referral_codes
                 WHERE code = :code
                   AND is_active = TRUE
                   AND (max_usage IS NULL OR usage_count < max_usage)
                """, Map.of("code", code), (rs, rowNum) -> new ReferralCodeValidationResponse(
                true,
                rs.getString("code"),
                rs.getString("code_type"),
                rs.getObject("owner_partner_id", Long.class),
                rs.getString("country"),
                rs.getString("city"),
                "VALID_CODE",
                "auth.referral.valid"
        ));
        return rows.stream().findFirst();
    }

    @Override
    public void createEmailVerification(String email, String codeHash, Instant expiresAt, String requestId) {
        jdbcTemplate.update("""
                INSERT INTO distributor_signup_email_verifications (
                    email, code_hash, status, request_id, expires_at
                )
                VALUES (:email, :codeHash, 'PENDING', :requestId, :expiresAt)
                """, new MapSqlParameterSource()
                .addValue("email", email)
                .addValue("codeHash", codeHash)
                .addValue("requestId", requestId)
                .addValue("expiresAt", Timestamp.from(expiresAt)));
    }

    @Override
    public boolean confirmEmailVerification(String email, String codeHash, Instant now) {
        int updated = jdbcTemplate.update("""
                UPDATE distributor_signup_email_verifications
                   SET status = 'VERIFIED',
                       verified_at = :now,
                       updated_at = :now
                 WHERE id = (
                     SELECT id
                       FROM distributor_signup_email_verifications
                      WHERE lower(email) = lower(:email)
                        AND code_hash = :codeHash
                        AND status = 'PENDING'
                        AND expires_at > :now
                      ORDER BY created_at DESC
                      LIMIT 1
                 )
                """, new MapSqlParameterSource()
                .addValue("email", email)
                .addValue("codeHash", codeHash)
                .addValue("now", Timestamp.from(now)));
        if (updated == 0) {
            jdbcTemplate.update("""
                    UPDATE distributor_signup_email_verifications
                       SET status = 'EXPIRED',
                           updated_at = :now
                     WHERE lower(email) = lower(:email)
                       AND status = 'PENDING'
                       AND expires_at <= :now
                    """, new MapSqlParameterSource()
                    .addValue("email", email)
                    .addValue("now", Timestamp.from(now)));
        }
        return updated > 0;
    }

    @Override
    public boolean emailVerified(String email) {
        return count("""
                SELECT COUNT(*)
                  FROM distributor_signup_email_verifications
                 WHERE lower(email) = lower(:email)
                   AND status = 'VERIFIED'
                   AND verified_at IS NOT NULL
                """, Map.of("email", email)) > 0;
    }

    @Override
    public void recordWalletVerification(
            WalletLinkVerifyRequest request,
            String signatureHash,
            String status,
            String errorCode,
            String errorMessage
    ) {
        jdbcTemplate.update("""
                INSERT INTO distributor_signup_wallet_verifications (
                    applicant_type, email, wallet_network, wallet_address, nonce,
                    signature_hash, auth_status, request_id, last_error_code,
                    last_error_message, verified_at
                )
                VALUES (
                    :applicantType, :email, 'TRON', :walletAddress, :nonce,
                    :signatureHash, :status, :requestId, :errorCode,
                    :errorMessage, CASE WHEN :status = 'VERIFIED' THEN CURRENT_TIMESTAMP ELSE NULL END
                )
                """, new MapSqlParameterSource()
                .addValue("applicantType", request.applicantType())
                .addValue("email", request.email())
                .addValue("walletAddress", request.walletAddress())
                .addValue("nonce", request.nonce())
                .addValue("signatureHash", signatureHash)
                .addValue("status", status)
                .addValue("requestId", request.requestId())
                .addValue("errorCode", errorCode)
                .addValue("errorMessage", errorMessage));
    }

    @Override
    public long createSignupApplication(SignupApplicationRequest request, String passwordHash) {
        ReferralCodeValidationResponse referral = request.referralCode() == null || request.referralCode().isBlank()
                ? null
                : findReferralCode(request.referralCode()).orElse(null);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("applicantType", request.applicantType())
                .addValue("loginId", request.loginId())
                .addValue("passwordHash", passwordHash)
                .addValue("email", request.email())
                .addValue("companyName", request.companyName())
                .addValue("contactName", request.contactName())
                .addValue("phone", request.phone())
                .addValue("telegram", request.telegram())
                .addValue("whatsapp", request.whatsapp())
                .addValue("referralCode", request.referralCode())
                .addValue("referralCodeId", referral == null ? null : findReferralCodeId(request.referralCode()))
                .addValue("applicationType", applicationType(request.applicantType()))
                .addValue("requestedRole", requestedRole(request.applicantType()))
                .addValue("contractPath", contractPath(request.applicantType(), referral))
                .addValue("country", request.country())
                .addValue("region", request.region())
                .addValue("city", request.city())
                .addValue("address", request.address())
                .addValue("businessType", request.businessType())
                .addValue("walletNetwork", request.walletAddress() == null || request.walletAddress().isBlank() ? null : "TRON")
                .addValue("walletAddress", request.walletAddress())
                .addValue("integrationPlan", request.integrationPlan())
                .addValue("evidenceNote", request.evidenceNote())
                .addValue("requestId", request.requestId());

        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO distributor_signup_applications (
                    application_type, applicant_type, login_id, password_hash, email, company_name,
                    contact_name, phone, telegram, whatsapp, referral_code,
                    referral_code_id, requested_role, contract_path,
                    country, region, city, address, business_type,
                    wallet_network, wallet_address, wallet_auth_status,
                    integration_plan, evidence_note, request_id, source, status
                )
                VALUES (
                    :applicationType, :applicantType, :loginId, :passwordHash, :email, :companyName,
                    :contactName, :phone, :telegram, :whatsapp, :referralCode,
                    :referralCodeId, :requestedRole, :contractPath,
                    :country, :region, :city, :address, :businessType,
                    :walletNetwork, :walletAddress, 'UNVERIFIED',
                    :integrationPlan, :evidenceNote, :requestId, 'PORTAL', 'REQUESTED'
                )
                RETURNING id
                """, params, Long.class);
        return id == null ? 0L : id;
    }

    @Override
    public void recordActivity(String role, String actionType, String status, String targetType, Long targetId, String requestId) {
        jdbcTemplate.update("""
                INSERT INTO distributor_activity_logs (
                    actor_role, action_type, action_status, target_type, target_id, request_id
                )
                VALUES (:role, :actionType, :status, :targetType, :targetId, :requestId)
                """, new MapSqlParameterSource()
                .addValue("role", role)
                .addValue("actionType", actionType)
                .addValue("status", status)
                .addValue("targetType", targetType)
                .addValue("targetId", targetId)
                .addValue("requestId", requestId));
    }

    private Long findReferralCodeId(String code) {
        return jdbcTemplate.queryForObject("""
                SELECT id FROM referral_codes WHERE code = :code
                """, Map.of("code", code), Long.class);
    }

    private long count(String sql, Map<String, ?> params) {
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count == null ? 0L : count;
    }

    private String requestedRole(String applicantType) {
        return "PARTNER".equals(applicantType) ? "SALES_PARTNER" : "MERCHANT";
    }

    private String applicationType(String applicantType) {
        return "PARTNER".equals(applicantType) ? "SALES_PARTNER" : "MERCHANT";
    }

    private String contractPath(String applicantType, ReferralCodeValidationResponse referral) {
        if (referral == null) {
            return "HQ_DIRECT";
        }
        if ("PARTNER".equals(applicantType)) {
            return "LEADER_DIRECT";
        }
        if ("COUNTRY_LEADER".equals(referral.codeType())) {
            return "LEADER_DIRECT";
        }
        return "PARTNER_DIRECT";
    }
}
