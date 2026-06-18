package com.korion.chong.leader;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSignupApprovalRepository implements SignupApprovalRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcSignupApprovalRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<SignupApplicationApprovalRecord> findApplicationForUpdate(long applicationId) {
        List<SignupApplicationApprovalRecord> rows = jdbcTemplate.query("""
                SELECT a.id,
                       a.applicant_type,
                       a.login_id,
                       a.password_hash,
                       a.email,
                       a.company_name,
                       a.contact_name,
                       a.phone,
                       a.referral_code,
                       rc.owner_partner_id,
                       owner.partner_type AS owner_partner_type,
                       CASE
                           WHEN owner.partner_type = 'COUNTRY_LEADER' THEN owner.id
                           WHEN owner.partner_type = 'SALES_PARTNER' THEN owner.parent_partner_id
                           ELSE NULL
                       END AS owner_leader_partner_id,
                       COALESCE(a.country, rc.country) AS country,
                       a.region,
                       COALESCE(a.city, rc.city) AS city,
                       a.address,
                       a.business_type,
                       a.wallet_network,
                       a.wallet_address,
                       a.wallet_auth_status,
                       a.contract_path,
                       a.status
                  FROM distributor_signup_applications a
             LEFT JOIN referral_codes rc ON rc.id = a.referral_code_id
             LEFT JOIN partners owner ON owner.id = rc.owner_partner_id
                 WHERE a.id = :applicationId
                 FOR UPDATE OF a
                """, Map.of("applicationId", applicationId), (rs, rowNum) -> new SignupApplicationApprovalRecord(
                rs.getLong("id"),
                rs.getString("applicant_type"),
                rs.getString("login_id"),
                rs.getString("password_hash"),
                rs.getString("email"),
                rs.getString("company_name"),
                rs.getString("contact_name"),
                rs.getString("phone"),
                rs.getString("referral_code"),
                nullableLong(rs, "owner_partner_id"),
                rs.getString("owner_partner_type"),
                nullableLong(rs, "owner_leader_partner_id"),
                rs.getString("country"),
                rs.getString("region"),
                rs.getString("city"),
                rs.getString("address"),
                rs.getString("business_type"),
                rs.getString("wallet_network"),
                rs.getString("wallet_address"),
                rs.getString("wallet_auth_status"),
                rs.getString("contract_path"),
                rs.getString("status")
        ));
        return rows.stream().findFirst();
    }

    @Override
    public long createUser(SignupApplicationApprovalRecord application) {
        List<Long> existing = jdbcTemplate.query("""
                SELECT id
                  FROM users
                 WHERE lower(login_id) = lower(:loginId)
                   AND deleted_at IS NULL
                 LIMIT 1
                """, Map.of("loginId", application.loginId()), (rs, rowNum) -> rs.getLong("id"));
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO users (login_id, password_hash, status)
                VALUES (:loginId, :passwordHash, 'ACTIVE')
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("loginId", application.loginId())
                .addValue("passwordHash", application.passwordHash()), Long.class);
        return id == null ? 0L : id;
    }

    @Override
    public long createPartner(SignupApplicationApprovalRecord application, long userId, long approvedByUserId, Long parentPartnerId, Instant now) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO partners (
                    user_id, partner_type, country, region, city, status,
                    parent_partner_id, approved_by, approved_at, application_type,
                    requested_role, current_stage, badge_type, badge_status,
                    assigned_country, store_access_status
                )
                VALUES (
                    :userId, 'SALES_PARTNER', :country, :region, :city, 'SALES_PARTNER_APPROVED',
                    :parentPartnerId, :approvedBy, :approvedAt, 'SALES_PARTNER',
                    'SALES_PARTNER', 'SALES_PARTNER_APPROVED', 'BLUE', 'ACTIVE',
                    :country, 'ALLOWED'
                )
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("country", application.country())
                .addValue("region", application.region())
                .addValue("city", application.city())
                .addValue("parentPartnerId", parentPartnerId)
                .addValue("approvedBy", approvedByUserId)
                .addValue("approvedAt", Timestamp.from(now)), Long.class);
        return id == null ? 0L : id;
    }

    @Override
    public long createMerchant(SignupApplicationApprovalRecord application, long userId, long approvedByUserId, Long leaderPartnerId, Long salesPartnerId, Instant now) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO merchants (
                    owner_user_id, merchant_name, business_type, country, region, city,
                    address, status, parent_country_master_id, parent_sales_partner_id,
                    merchant_code, approved_by, approved_at, store_access_status
                )
                VALUES (
                    :userId, :merchantName, :businessType, :country, :region, :city,
                    :address, 'MERCHANT_APPROVED', :leaderPartnerId, :salesPartnerId,
                    :merchantCode, :approvedBy, :approvedAt, 'ALLOWED'
                )
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("merchantName", application.companyName())
                .addValue("businessType", application.businessType())
                .addValue("country", application.country())
                .addValue("region", application.region())
                .addValue("city", application.city())
                .addValue("address", application.address())
                .addValue("leaderPartnerId", leaderPartnerId)
                .addValue("salesPartnerId", salesPartnerId)
                .addValue("merchantCode", "MER-" + application.applicationId())
                .addValue("approvedBy", approvedByUserId)
                .addValue("approvedAt", Timestamp.from(now)), Long.class);
        return id == null ? 0L : id;
    }

    @Override
    public long createContract(SignupApplicationApprovalRecord application, long approvedByUserId, Long leaderPartnerId, Long salesPartnerId, Long merchantId, Instant now) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO distributor_contracts (
                    contract_path, leader_partner_id, sales_partner_id, merchant_id,
                    status, created_by, created_at, updated_at
                )
                VALUES (
                    :contractPath, :leaderPartnerId, :salesPartnerId, :merchantId,
                    'ACTIVE', :createdBy, :now, :now
                )
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("contractPath", runtimeContractPath(application, merchantId))
                .addValue("leaderPartnerId", leaderPartnerId)
                .addValue("salesPartnerId", salesPartnerId)
                .addValue("merchantId", merchantId)
                .addValue("createdBy", approvedByUserId)
                .addValue("now", Timestamp.from(now)), Long.class);
        return id == null ? 0L : id;
    }

    @Override
    public Long createWalletAddress(SignupApplicationApprovalRecord application, long userId, Long partnerId, Long merchantId, Instant now) {
        if (application.walletAddress() == null || application.walletAddress().isBlank()) {
            return null;
        }
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO distributor_wallet_addresses (
                    owner_type, owner_user_id, partner_id, merchant_id, network,
                    currency_code, address, auth_status, verified_at, created_at, updated_at
                )
                VALUES (
                    :ownerType, :ownerUserId, :partnerId, :merchantId, 'TRON',
                    'TRX', :address, :authStatus, :verifiedAt, :now, :now
                )
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("ownerType", partnerId == null ? "MERCHANT" : "PARTNER")
                .addValue("ownerUserId", userId)
                .addValue("partnerId", partnerId)
                .addValue("merchantId", merchantId)
                .addValue("address", application.walletAddress())
                .addValue("authStatus", "VERIFIED".equals(application.walletAuthStatus()) ? "VERIFIED" : "UNVERIFIED")
                .addValue("verifiedAt", "VERIFIED".equals(application.walletAuthStatus()) ? Timestamp.from(now) : null)
                .addValue("now", Timestamp.from(now)), Long.class);
        return id;
    }

    @Override
    public void approveApplication(long applicationId, Instant now) {
        jdbcTemplate.update("""
                UPDATE distributor_signup_applications
                   SET status = 'APPROVED',
                       activated_at = :now,
                       updated_at = :now
                 WHERE id = :applicationId
                """, new MapSqlParameterSource()
                .addValue("applicationId", applicationId)
                .addValue("now", Timestamp.from(now)));
    }

    @Override
    public void rejectApplication(long applicationId, String reason, Instant now) {
        jdbcTemplate.update("""
                UPDATE distributor_signup_applications
                   SET status = 'REJECTED',
                       inactivated_at = :now,
                       evidence_note = CASE
                           WHEN :reason IS NULL OR :reason = '' THEN evidence_note
                           ELSE concat(COALESCE(evidence_note, ''), CASE WHEN evidence_note IS NULL OR evidence_note = '' THEN '' ELSE E'\\n' END, 'REJECTED: ', :reason)
                       END,
                       updated_at = :now
                 WHERE id = :applicationId
                """, new MapSqlParameterSource()
                .addValue("applicationId", applicationId)
                .addValue("reason", reason)
                .addValue("now", Timestamp.from(now)));
    }

    @Override
    public long findUserIdByLeaderId(long leaderId) {
        Long userId = jdbcTemplate.queryForObject("""
                SELECT user_id FROM partners WHERE id = :leaderId
                """, Map.of("leaderId", leaderId), Long.class);
        return userId == null ? 0L : userId;
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

    private String runtimeContractPath(SignupApplicationApprovalRecord application, Long merchantId) {
        if (merchantId == null) {
            return "LEADER_PARTNER";
        }
        if ("PARTNER_DIRECT".equals(application.contractPath())) {
            return "PARTNER_MERCHANT";
        }
        if ("LEADER_DIRECT".equals(application.contractPath())) {
            return "LEADER_MERCHANT";
        }
        return "HQ_MERCHANT";
    }

    private static Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
