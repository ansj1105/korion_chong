package com.korion.chong.settlement;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSettlementRepository implements SettlementRepository {
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcSettlementRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public BigDecimal availablePartnerAmount(long partnerId, LocalDate periodStart, LocalDate periodEnd) {
        BigDecimal amount = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(commission_amount), 0)
                  FROM distributor_commission_entries
                 WHERE beneficiary_partner_id = :partnerId
                   AND beneficiary_type IN ('LEADER', 'PARTNER')
                   AND settlement_status IN ('PENDING', 'AVAILABLE')
                   AND source_status = 'CONFIRMED'
                   AND occurred_at >= :periodStart
                   AND occurred_at < :periodEndExclusive
                """, periodParams(partnerId, periodStart, periodEnd), BigDecimal.class);
        return amount == null ? ZERO : amount;
    }

    @Override
    public long createPartnerRequest(
            long requesterUserId,
            String recipientType,
            long recipientPartnerId,
            Long walletAddressId,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal requestedAmount,
            String memo,
            Instant now
    ) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO distributor_settlement_requests (
                    request_no, requester_user_id, recipient_type, recipient_partner_id,
                    wallet_address_id, period_start, period_end, requested_amount,
                    approved_amount, held_amount, status, memo, requested_at, created_at, updated_at
                )
                VALUES (
                    :requestNo, :requesterUserId, :recipientType, :recipientPartnerId,
                    :walletAddressId, :periodStart, :periodEnd, :requestedAmount,
                    0, 0, 'REQUESTED', :memo, :now, :now, :now
                )
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("requestNo", "SET-" + now.toEpochMilli() + "-" + recipientPartnerId)
                .addValue("requesterUserId", requesterUserId)
                .addValue("recipientType", recipientType)
                .addValue("recipientPartnerId", recipientPartnerId)
                .addValue("walletAddressId", walletAddressId)
                .addValue("periodStart", periodStart)
                .addValue("periodEnd", periodEnd)
                .addValue("requestedAmount", requestedAmount)
                .addValue("memo", memo)
                .addValue("now", Timestamp.from(now)), Long.class);
        return id == null ? 0L : id;
    }

    @Override
    public void attachPartnerCommissions(long settlementRequestId, long partnerId, LocalDate periodStart, LocalDate periodEnd) {
        jdbcTemplate.update("""
                UPDATE distributor_commission_entries
                   SET settlement_status = 'REQUESTED',
                       settlement_request_id = :settlementRequestId,
                       updated_at = CURRENT_TIMESTAMP
                 WHERE beneficiary_partner_id = :partnerId
                   AND beneficiary_type IN ('LEADER', 'PARTNER')
                   AND settlement_status IN ('PENDING', 'AVAILABLE')
                   AND source_status = 'CONFIRMED'
                   AND occurred_at >= :periodStart
                   AND occurred_at < :periodEndExclusive
                """, periodParams(partnerId, periodStart, periodEnd)
                .addValue("settlementRequestId", settlementRequestId));
    }

    @Override
    public Optional<SettlementRequestRecord> findForUpdate(long settlementRequestId) {
        List<SettlementRequestRecord> rows = jdbcTemplate.query("""
                SELECT id, request_no, requester_user_id, recipient_type, recipient_partner_id,
                       recipient_merchant_id, wallet_address_id, period_start, period_end,
                       currency, requested_amount, approved_amount, held_amount, status
                  FROM distributor_settlement_requests
                 WHERE id = :settlementRequestId
                 FOR UPDATE
                """, Map.of("settlementRequestId", settlementRequestId), (rs, rowNum) -> record(rs));
        return rows.stream().findFirst();
    }

    @Override
    public boolean canLeaderManage(long leaderPartnerId, SettlementRequestRecord request) {
        if ("PARTNER".equals(request.recipientType()) && request.recipientPartnerId() != null) {
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                      FROM partners
                     WHERE id = :partnerId
                       AND parent_partner_id = :leaderPartnerId
                    """, new MapSqlParameterSource()
                    .addValue("partnerId", request.recipientPartnerId())
                    .addValue("leaderPartnerId", leaderPartnerId), Long.class);
            return count != null && count > 0;
        }
        if ("MERCHANT".equals(request.recipientType()) && request.recipientMerchantId() != null) {
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                      FROM merchants
                     WHERE id = :merchantId
                       AND parent_country_master_id = :leaderPartnerId
                    """, new MapSqlParameterSource()
                    .addValue("merchantId", request.recipientMerchantId())
                    .addValue("leaderPartnerId", leaderPartnerId), Long.class);
            return count != null && count > 0;
        }
        return false;
    }

    @Override
    public SettlementRequestRecord approve(long settlementRequestId, long reviewedByUserId, BigDecimal approvedAmount, BigDecimal heldAmount, String note, Instant now) {
        jdbcTemplate.update("""
                UPDATE distributor_settlement_requests
                   SET status = 'APPROVED',
                       approved_amount = :approvedAmount,
                       held_amount = :heldAmount,
                       reviewed_by = :reviewedBy,
                       reviewed_at = :now,
                       review_note = :note,
                       updated_at = :now
                 WHERE id = :settlementRequestId
                """, actionParams(settlementRequestId, reviewedByUserId, now)
                .addValue("approvedAmount", approvedAmount)
                .addValue("heldAmount", heldAmount)
                .addValue("note", note));
        jdbcTemplate.update("""
                UPDATE distributor_commission_entries
                   SET settlement_status = CASE
                       WHEN :approvedAmount > 0 THEN 'APPROVED'
                       ELSE 'HELD'
                   END,
                       updated_at = :now
                 WHERE settlement_request_id = :settlementRequestId
                """, new MapSqlParameterSource()
                .addValue("settlementRequestId", settlementRequestId)
                .addValue("approvedAmount", approvedAmount)
                .addValue("now", Timestamp.from(now)));
        return findForUpdate(settlementRequestId).orElseThrow();
    }

    @Override
    public SettlementRequestRecord reject(long settlementRequestId, long reviewedByUserId, String note, Instant now) {
        jdbcTemplate.update("""
                UPDATE distributor_settlement_requests
                   SET status = 'REJECTED',
                       reviewed_by = :reviewedBy,
                       reviewed_at = :now,
                       review_note = :note,
                       updated_at = :now
                 WHERE id = :settlementRequestId
                """, actionParams(settlementRequestId, reviewedByUserId, now).addValue("note", note));
        jdbcTemplate.update("""
                UPDATE distributor_commission_entries
                   SET settlement_status = 'REJECTED',
                       updated_at = :now
                 WHERE settlement_request_id = :settlementRequestId
                """, new MapSqlParameterSource()
                .addValue("settlementRequestId", settlementRequestId)
                .addValue("now", Timestamp.from(now)));
        return findForUpdate(settlementRequestId).orElseThrow();
    }

    @Override
    public SettlementRequestRecord markPaid(long settlementRequestId, long reviewedByUserId, Instant now) {
        jdbcTemplate.update("""
                UPDATE distributor_settlement_requests
                   SET status = 'PAID',
                       reviewed_by = COALESCE(reviewed_by, :reviewedBy),
                       paid_at = :now,
                       updated_at = :now
                 WHERE id = :settlementRequestId
                """, actionParams(settlementRequestId, reviewedByUserId, now));
        jdbcTemplate.update("""
                UPDATE distributor_commission_entries
                   SET settlement_status = 'PAID',
                       updated_at = :now
                 WHERE settlement_request_id = :settlementRequestId
                """, new MapSqlParameterSource()
                .addValue("settlementRequestId", settlementRequestId)
                .addValue("now", Timestamp.from(now)));
        return findForUpdate(settlementRequestId).orElseThrow();
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

    private MapSqlParameterSource periodParams(long partnerId, LocalDate periodStart, LocalDate periodEnd) {
        return new MapSqlParameterSource()
                .addValue("partnerId", partnerId)
                .addValue("periodStart", periodStart)
                .addValue("periodEndExclusive", periodEnd.plusDays(1));
    }

    private MapSqlParameterSource actionParams(long settlementRequestId, long reviewedByUserId, Instant now) {
        return new MapSqlParameterSource()
                .addValue("settlementRequestId", settlementRequestId)
                .addValue("reviewedBy", reviewedByUserId)
                .addValue("now", Timestamp.from(now));
    }

    private SettlementRequestRecord record(ResultSet rs) throws SQLException {
        return new SettlementRequestRecord(
                rs.getLong("id"),
                rs.getString("request_no"),
                nullableLong(rs, "requester_user_id"),
                rs.getString("recipient_type"),
                nullableLong(rs, "recipient_partner_id"),
                nullableLong(rs, "recipient_merchant_id"),
                nullableLong(rs, "wallet_address_id"),
                rs.getDate("period_start").toLocalDate(),
                rs.getDate("period_end").toLocalDate(),
                rs.getString("currency"),
                amount(rs, "requested_amount"),
                amount(rs, "approved_amount"),
                amount(rs, "held_amount"),
                rs.getString("status")
        );
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static BigDecimal amount(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? ZERO : value;
    }
}
