package com.korion.chong.auth;

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
                   AND status IN ('SUBMITTED', 'REVIEWING', 'NEED_MORE_INFO', 'HELD')
                """, Map.of("loginId", loginId)) > 0;
    }

    @Override
    public boolean applicationEmailExists(String email) {
        return count("""
                SELECT COUNT(*)
                  FROM distributor_signup_applications
                 WHERE lower(email) = lower(:email)
                   AND status IN ('SUBMITTED', 'REVIEWING', 'NEED_MORE_INFO', 'HELD')
                """, Map.of("email", email)) > 0;
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
                   AND status IN ('SUBMITTED', 'REVIEWING', 'NEED_MORE_INFO', 'HELD')
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
    public long createSignupApplication(SignupApplicationRequest request) {
        ReferralCodeValidationResponse referral = request.referralCode() == null || request.referralCode().isBlank()
                ? null
                : findReferralCode(request.referralCode()).orElse(null);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("applicantType", request.applicantType())
                .addValue("loginId", request.loginId())
                .addValue("email", request.email())
                .addValue("companyName", request.companyName())
                .addValue("contactName", request.contactName())
                .addValue("phone", request.phone())
                .addValue("referralCode", request.referralCode())
                .addValue("referralCodeId", referral == null ? null : findReferralCodeId(request.referralCode()))
                .addValue("requestedRole", requestedRole(request.applicantType()))
                .addValue("contractPath", contractPath(request.applicantType(), referral))
                .addValue("country", request.country())
                .addValue("region", request.region())
                .addValue("city", request.city())
                .addValue("walletNetwork", request.walletAddress() == null || request.walletAddress().isBlank() ? null : "TRON")
                .addValue("walletAddress", request.walletAddress())
                .addValue("integrationPlan", request.integrationPlan())
                .addValue("requestId", request.requestId());

        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO distributor_signup_applications (
                    applicant_type, login_id, email, company_name, contact_name, phone,
                    referral_code, referral_code_id, requested_role, contract_path,
                    country, region, city, wallet_network, wallet_address, wallet_auth_status,
                    integration_plan, request_id, status
                )
                VALUES (
                    :applicantType, :loginId, :email, :companyName, :contactName, :phone,
                    :referralCode, :referralCodeId, :requestedRole, :contractPath,
                    :country, :region, :city, :walletNetwork, :walletAddress, 'UNVERIFIED',
                    :integrationPlan, :requestId, 'SUBMITTED'
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
