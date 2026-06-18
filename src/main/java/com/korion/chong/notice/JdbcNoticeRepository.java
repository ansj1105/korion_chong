package com.korion.chong.notice;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcNoticeRepository implements NoticeRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcNoticeRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<NoticeRecipientRecord> findLeaderRecipient(long leaderPartnerId, NoticeRecipientRequest recipient) {
        if ("PARTNER".equals(recipient.recipientType()) && recipient.recipientPartnerId() != null) {
            List<NoticeRecipientRecord> rows = jdbcTemplate.query("""
                    SELECT user_id AS recipient_user_id, id AS recipient_partner_id
                      FROM partners
                     WHERE id = :partnerId
                       AND parent_partner_id = :leaderPartnerId
                    """, new MapSqlParameterSource()
                    .addValue("partnerId", recipient.recipientPartnerId())
                    .addValue("leaderPartnerId", leaderPartnerId), (rs, rowNum) -> new NoticeRecipientRecord(
                    "PARTNER",
                    nullableLong(rs, "recipient_user_id"),
                    nullableLong(rs, "recipient_partner_id"),
                    null
            ));
            return rows.stream().findFirst();
        }
        if ("MERCHANT".equals(recipient.recipientType()) && recipient.recipientMerchantId() != null) {
            List<NoticeRecipientRecord> rows = jdbcTemplate.query("""
                    SELECT owner_user_id AS recipient_user_id, id AS recipient_merchant_id
                      FROM merchants
                     WHERE id = :merchantId
                       AND parent_country_master_id = :leaderPartnerId
                    """, new MapSqlParameterSource()
                    .addValue("merchantId", recipient.recipientMerchantId())
                    .addValue("leaderPartnerId", leaderPartnerId), (rs, rowNum) -> new NoticeRecipientRecord(
                    "MERCHANT",
                    nullableLong(rs, "recipient_user_id"),
                    null,
                    nullableLong(rs, "recipient_merchant_id")
            ));
            return rows.stream().findFirst();
        }
        return Optional.empty();
    }

    @Override
    public Optional<NoticeRecipientRecord> findPartnerRecipient(long partnerId, NoticeRecipientRequest recipient) {
        if (!"MERCHANT".equals(recipient.recipientType()) || recipient.recipientMerchantId() == null) {
            return Optional.empty();
        }
        List<NoticeRecipientRecord> rows = jdbcTemplate.query("""
                SELECT owner_user_id AS recipient_user_id, id AS recipient_merchant_id
                  FROM merchants
                 WHERE id = :merchantId
                   AND parent_sales_partner_id = :partnerId
                """, new MapSqlParameterSource()
                .addValue("merchantId", recipient.recipientMerchantId())
                .addValue("partnerId", partnerId), (rs, rowNum) -> new NoticeRecipientRecord(
                "MERCHANT",
                nullableLong(rs, "recipient_user_id"),
                null,
                nullableLong(rs, "recipient_merchant_id")
        ));
        return rows.stream().findFirst();
    }

    @Override
    public long createNotice(String senderType, long senderUserId, long senderPartnerId, NoticeSendRequest request, String status, Instant now) {
        Instant sentAt = "SENT".equals(status) ? now : null;
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO distributor_notices (
                    sender_type, sender_user_id, sender_partner_id, title,
                    notice_type, body, channel, status, scheduled_at, sent_at,
                    created_at, updated_at
                )
                VALUES (
                    :senderType, :senderUserId, :senderPartnerId, :title,
                    :noticeType, :body, :channel, :status, :scheduledAt, :sentAt,
                    :now, :now
                )
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("senderType", senderType)
                .addValue("senderUserId", senderUserId)
                .addValue("senderPartnerId", senderPartnerId)
                .addValue("title", request.title())
                .addValue("noticeType", request.noticeType())
                .addValue("body", request.body())
                .addValue("channel", request.channel())
                .addValue("status", status)
                .addValue("scheduledAt", timestamp(request.scheduledAt()))
                .addValue("sentAt", timestamp(sentAt))
                .addValue("now", Timestamp.from(now)), Long.class);
        return id == null ? 0L : id;
    }

    @Override
    public void createRecipients(long noticeId, List<NoticeRecipientRecord> recipients, String deliveryStatus, Instant deliveredAt) {
        Timestamp deliveredTimestamp = timestamp(deliveredAt);
        for (NoticeRecipientRecord recipient : recipients) {
            jdbcTemplate.update("""
                    INSERT INTO distributor_notice_recipients (
                        notice_id, recipient_type, recipient_user_id, recipient_partner_id,
                        recipient_merchant_id, delivery_status, read_status, delivered_at,
                        created_at, updated_at
                    )
                    VALUES (
                        :noticeId, :recipientType, :recipientUserId, :recipientPartnerId,
                        :recipientMerchantId, :deliveryStatus, 'UNREAD', :deliveredAt,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                    )
                    """, new MapSqlParameterSource()
                    .addValue("noticeId", noticeId)
                    .addValue("recipientType", recipient.recipientType())
                    .addValue("recipientUserId", recipient.recipientUserId())
                    .addValue("recipientPartnerId", recipient.recipientPartnerId())
                    .addValue("recipientMerchantId", recipient.recipientMerchantId())
                    .addValue("deliveryStatus", deliveryStatus)
                    .addValue("deliveredAt", deliveredTimestamp));
        }
    }

    @Override
    public void recordActivity(String role, String actionType, String status, long noticeId, String requestId) {
        jdbcTemplate.update("""
                INSERT INTO distributor_activity_logs (
                    actor_role, action_type, action_status, target_type, target_id, request_id
                )
                VALUES (:role, :actionType, :status, 'distributor_notices', :noticeId, :requestId)
                """, new MapSqlParameterSource()
                .addValue("role", role)
                .addValue("actionType", actionType)
                .addValue("status", status)
                .addValue("noticeId", noticeId)
                .addValue("requestId", requestId));
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
