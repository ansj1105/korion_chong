package com.korion.chong.merchant;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMerchantDashboardRepository implements MerchantDashboardRepository {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcMerchantDashboardRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<MerchantProfile> findMerchantProfile(long merchantId) {
        List<MerchantProfile> rows = jdbcTemplate.query("""
                SELECT m.id, m.owner_user_id, u.login_id, m.merchant_name, m.status,
                       m.country, m.region, m.city
                  FROM merchants m
                  JOIN users u ON u.id = m.owner_user_id
                 WHERE m.id = :merchantId
                   AND m.status = 'MERCHANT_APPROVED'
                   AND m.store_access_status = 'ALLOWED'
                """, Map.of("merchantId", merchantId), (rs, rowNum) -> new MerchantProfile(
                rs.getLong("id"),
                rs.getLong("owner_user_id"),
                rs.getString("login_id"),
                rs.getString("merchant_name"),
                rs.getString("status"),
                rs.getString("country"),
                rs.getString("region"),
                rs.getString("city")
        ));
        return rows.stream().findFirst();
    }

    @Override
    public Map<String, Object> findDashboard(long merchantId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
        MapSqlParameterSource params = periodParams(merchantId, countryScope, periodStart, periodEnd);
        Map<String, Object> summary = jdbcTemplate.queryForMap("""
                SELECT
                    COALESCE(SUM(CASE WHEN kp.status = 'COMPLETED' AND kp.paid_at::date = CURRENT_DATE THEN kp.amount ELSE 0 END), 0) AS today_sales,
                    COALESCE(SUM(CASE WHEN kp.status = 'COMPLETED' THEN kp.amount ELSE 0 END), 0) AS month_sales,
                    COUNT(CASE WHEN kp.status = 'COMPLETED' THEN 1 END) AS month_count,
                    COUNT(CASE WHEN kp.status IN ('FAILED', 'CANCELLED', 'REFUNDED') THEN 1 END) AS fail_count
                  FROM merchants m
             LEFT JOIN korion_payments kp ON kp.merchant_id = m.id
                   AND kp.paid_at >= :periodStart
                   AND kp.paid_at < :periodEnd
                 WHERE m.id = :merchantId
                   AND m.country = :countryScope
                """, params);
        long unreadNoticeCount = countBy("""
                SELECT COUNT(*)
                  FROM distributor_notice_recipients nr
                 WHERE nr.recipient_type = 'MERCHANT'
                   AND nr.recipient_merchant_id = :merchantId
                   AND nr.read_status = 'UNREAD'
                """, baseParams(merchantId, countryScope));
        return Map.of("kpis", List.of(
                kpi("products", "mdash.kpi.products", productStatus(merchantId), null, null, "orange"),
                kpi("today-sales", "mdash.kpi.todaySales", kori(asBigDecimal(summary.get("today_sales"))), null, "매장", "cyan"),
                kpi("month-sales", "mdash.kpi.monthSales", kori(asBigDecimal(summary.get("month_sales"))), null, "관리", "green"),
                kpi("month-count", "mdash.kpi.monthCount", summary.get("month_count"), null, null, "green"),
                kpi("sync-fail", "mdash.kpi.syncFail", summary.get("fail_count") + "건", null, "확인", "orange"),
                kpi("unread-notice", "mdash.kpi.unreadNotice", unreadNoticeCount + "건", null, null, "orange")
        ));
    }

    @Override
    public Map<String, Object> findTransactions(long merchantId, String countryScope, LocalDate periodStart, LocalDate periodEnd, String variant) {
        MapSqlParameterSource params = periodParams(merchantId, countryScope, periodStart, periodEnd)
                .addValue("variant", variant);
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT kp.id, kp.amount, kp.total_fee, kp.net_amount_to_merchant,
                       kp.status, kp.paid_at, kp.currency, m.merchant_code, m.merchant_name
                  FROM korion_payments kp
                  JOIN merchants m ON m.id = kp.merchant_id
                 WHERE kp.merchant_id = :merchantId
                   AND m.country = :countryScope
                   AND kp.paid_at >= :periodStart
                   AND kp.paid_at < :periodEnd
                   AND (:variant <> 'refund' OR kp.status IN ('REFUNDED', 'CANCELLED'))
                 ORDER BY kp.paid_at DESC NULLS LAST, kp.id DESC
                 LIMIT 50
                """, params, this::transactionRow);
        BigDecimal monthRevenue = rows.stream()
                .map(row -> new BigDecimal(String.valueOf(row.get("amount")).replace(" KORI", "")))
                .reduce(ZERO, BigDecimal::add);
        return Map.of(
                "stats", List.of(
                        stat("todayVolume", "partnerSales.stat.todayVolume", "0 KORI"),
                        stat("todayCount", "partnerSales.stat.todayCount", "0"),
                        stat("monthRevenue", "partnerSales.stat.monthRevenue", kori(monthRevenue)),
                        stat("monthCount", "partnerSales.stat.monthCount", rows.size()),
                        stat("monthFee", "partnerSales.stat.monthFee", sumRows(rows, "fee") + " KORI"),
                        stat("unsettledFee", "partnerSales.stat.unsettledFee", sumRows(rows, "unsettledFee")),
                        stat("offlineRatio", "partnerSales.stat.offlineRatio", "0/0/0"),
                        stat("failCancel", "partnerSales.stat.failCancel", failCount(rows))
                ),
                "t1Rows", rows,
                "t2Rows", rows
        );
    }

    @Override
    public Map<String, Object> findStore(long merchantId, String countryScope) {
        return profilePayload(merchantId, countryScope);
    }

    @Override
    public Map<String, Object> findProfile(long merchantId, String countryScope) {
        return profilePayload(merchantId, countryScope);
    }

    @Override
    public Map<String, Object> findHqNotices(long merchantId, String countryScope) {
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT n.id, n.sender_type, n.title, n.sent_at, n.created_at, nr.read_status
                  FROM distributor_notice_recipients nr
                  JOIN distributor_notices n ON n.id = nr.notice_id
                 WHERE nr.recipient_type = 'MERCHANT'
                   AND nr.recipient_merchant_id = :merchantId
                 ORDER BY COALESCE(n.sent_at, n.created_at) DESC, n.id DESC
                 LIMIT 50
                """, baseParams(merchantId, countryScope), (rs, rowNum) -> {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("no", "N-" + rs.getLong("id"));
            row.put("author", noticeAuthor(rs.getString("sender_type")));
            row.put("target", "가맹점");
            row.put("title", rs.getString("title"));
            row.put("sentDate", date(coalesceTimestamp(rs, "sent_at", "created_at")));
            row.put("read", "READ".equals(rs.getString("read_status")) ? "읽음" : "확인필요");
            return row;
        });
        return Map.of("rows", rows);
    }

    @Override
    public Map<String, Object> findActivityLogs(long merchantId, String countryScope) {
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, action_type, action_status, target_type, target_id, ip, user_agent, created_at
                  FROM distributor_activity_logs
                 WHERE actor_merchant_id = :merchantId
                    OR (actor_role = 'MERCHANT' AND target_type = 'merchants' AND target_id = :merchantId)
                 ORDER BY created_at DESC, id DESC
                 LIMIT 50
                """, baseParams(merchantId, countryScope), (rs, rowNum) -> activityRow(rs, rowNum));
        return Map.of(
                "metrics", List.of(
                        metric("recent", "act.kpi.recent", rows.isEmpty() ? "-" : String.valueOf(rows.get(0).get("datetime")), null, "#2a8bff", false),
                        metric("visits", "act.kpi.visits", countAction(rows, "LOGIN") + "회", "act.kpi.visits.note", "#24e6b8", true),
                        metric("notices", "act.kpi.notices", countAction(rows, "NOTICE") + "건", "act.kpi.notices.note", "#7c5cff", false),
                        metric("failed", "act.kpi.failed", countStatus(rows, "실패") + "건", "act.kpi.failed.note", "#ff4d70", false)
                ),
                "rows", rows
        );
    }

    @Override
    public Map<String, Object> findSettlementHistory(long merchantId, String countryScope) {
        MapSqlParameterSource params = baseParams(merchantId, countryScope);
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT sr.request_no, sr.period_start, sr.period_end, sr.requested_amount,
                       sr.held_amount, sr.status, sr.requested_at, sr.paid_at,
                       COALESCE(SUM(ce.gross_amount), 0) AS total_amount
                  FROM distributor_settlement_requests sr
             LEFT JOIN distributor_commission_entries ce ON ce.settlement_request_id = sr.id
                 WHERE sr.recipient_type = 'MERCHANT'
                   AND sr.recipient_merchant_id = :merchantId
                 GROUP BY sr.id, sr.request_no, sr.period_start, sr.period_end,
                          sr.requested_amount, sr.held_amount, sr.status, sr.requested_at, sr.paid_at
                 ORDER BY sr.requested_at DESC, sr.id DESC
                 LIMIT 50
                """, params, this::settlementHistoryRow);
        return Map.of("lastSettleDate", "-", "thisRequestAmount", "0 KORI", "rows", rows);
    }

    @Override
    public Map<String, Object> findSettlementDetail(long merchantId, String countryScope) {
        return Map.of("no", "-", "status", "내역 없음", "basicInfo", List.of(), "amountSummary", List.of(), "merchantRows", List.of(), "heldRows", List.of());
    }

    private Map<String, Object> profilePayload(long merchantId, String countryScope) {
        return jdbcTemplate.queryForObject("""
                SELECT m.id, m.merchant_code, m.merchant_name, m.business_type, m.country, m.region, m.city,
                       m.address, m.status, m.store_access_status, m.created_at, m.approved_at,
                       u.login_id, u.name, u.nickname, u.phone,
                       sp.id AS partner_id,
                       dw.address AS wallet_address,
                       dw.auth_status AS wallet_status,
                       msp.product_status, msp.qr_status, msp.nfc_status, msp.ble_status, msp.review_status
                  FROM merchants m
                  JOIN users u ON u.id = m.owner_user_id
             LEFT JOIN partners sp ON sp.id = m.parent_sales_partner_id
             LEFT JOIN distributor_wallet_addresses dw ON dw.merchant_id = m.id
                   AND dw.auth_status <> 'REVOKED'
             LEFT JOIN merchant_store_profiles msp ON msp.merchant_id = m.id
                 WHERE m.id = :merchantId
                   AND m.country = :countryScope
                 ORDER BY dw.verified_at DESC NULLS LAST, dw.id DESC
                 LIMIT 1
                """, baseParams(merchantId, countryScope), (rs, rowNum) -> {
            String code = valueOrDash(rs.getString("merchant_code"), merchantCode(rs.getLong("id")));
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
            result.put("statusItems", List.of(
                    status("profile.status.leaderCode", partnerRef(rs)),
                    status("profile.status.country", rs.getString("country")),
                    status("profile.status.city", valueOrDash(rs.getString("city"), rs.getString("region"))),
                    status("profile.status.approval", merchantStatus(rs.getString("status"))),
                    status("profile.status.operation", storeAccessStatus(rs.getString("store_access_status"))),
                    status("profile.status.appliedDate", date(rs.getTimestamp("created_at"))),
                    status("profile.status.approvedDate", date(rs.getTimestamp("approved_at")))
            ));
            result.put("code", code);
            result.put("accountFields", List.of(
                    field("profile.acc.id", rs.getString("login_id")),
                    field("profile.acc.name", valueOrDash(rs.getString("name"), rs.getString("nickname"))),
                    field("profile.acc.email", "-"),
                    field("profile.acc.telegram", "-"),
                    field("profile.acc.phone", valueOrDash(rs.getString("phone"))),
                    field("profile.acc.twitter", "-"),
                    wideField("profile.acc.wallet", valueOrDash(rs.getString("wallet_address"))),
                    field("profile.acc.lastActive", "-")
            ));
            result.put("basicFields", List.of(
                    field("profile.basic.leaderCode", partnerRef(rs)),
                    field("profile.basic.country", rs.getString("country")),
                    field("profile.basic.region", valueOrDash(rs.getString("region"))),
                    field("profile.basic.language", "-"),
                    field("profile.basic.merchantCount", "1"),
                    field("profile.basic.unpaidFee", "-")
            ));
            result.put("store", Map.of(
                    "merchantName", rs.getString("merchant_name"),
                    "address", valueOrDash(rs.getString("address")),
                    "businessType", valueOrDash(rs.getString("business_type")),
                    "productStatus", valueOrDash(rs.getString("product_status")),
                    "qrStatus", valueOrDash(rs.getString("qr_status")),
                    "nfcStatus", valueOrDash(rs.getString("nfc_status")),
                    "bleStatus", valueOrDash(rs.getString("ble_status")),
                    "reviewStatus", valueOrDash(rs.getString("review_status")),
                    "walletStatus", valueOrDash(rs.getString("wallet_status"))
            ));
            return result;
        });
    }

    private Map<String, Object> transactionRow(ResultSet rs, int rowNum) throws SQLException {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("no", String.valueOf(rowNum + 1));
        row.put("code", valueOrDash(rs.getString("merchant_code"), merchantCode(rs.getLong("id"))));
        row.put("name", rs.getString("merchant_name"));
        row.put("telegram", "-");
        row.put("region", "-");
        row.put("monthRevenue", kori(rs, "amount"));
        row.put("monthCount", "1");
        row.put("recentActivity", date(rs.getTimestamp("paid_at")));
        row.put("partner", "-");
        row.put("merchantCode", valueOrDash(rs.getString("merchant_code"), merchantCode(rs.getLong("id"))));
        row.put("merchantName", rs.getString("merchant_name"));
        row.put("amount", kori(rs, "amount"));
        row.put("recentPay", kori(rs, "amount"));
        row.put("fee", amountText(amount(rs, "total_fee")));
        row.put("unsettledFee", kori(rs, "net_amount_to_merchant"));
        row.put("recentPay2", date(rs.getTimestamp("paid_at")));
        row.put("qrUsage", "QR");
        row.put("status", paymentStatus(rs.getString("status")));
        return row;
    }

    private Map<String, Object> activityRow(ResultSet rs, int rowNum) throws SQLException {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("no", String.valueOf(rowNum + 1));
        row.put("logId", "LOG-" + rs.getLong("id"));
        row.put("datetime", dateTime(rs.getTimestamp("created_at")));
        row.put("type", actionType(rs.getString("action_type")));
        row.put("menu", valueOrDash(rs.getString("target_type")));
        row.put("task", rs.getString("action_type"));
        row.put("target", valueOrDash(String.valueOf(rs.getObject("target_id"))));
        row.put("ip", valueOrDash(rs.getString("ip")));
        row.put("device", valueOrDash(rs.getString("user_agent")));
        row.put("status", actionStatus(rs.getString("action_status")));
        return row;
    }

    private Map<String, Object> settlementHistoryRow(ResultSet rs, int rowNum) throws SQLException {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("no", rs.getString("request_no"));
        row.put("appliedDate", date(rs.getTimestamp("requested_at")));
        row.put("period", DATE.format(rs.getDate("period_start").toLocalDate()) + "~ " + DATE.format(rs.getDate("period_end").toLocalDate()));
        row.put("totalAmount", kori(rs, "total_amount"));
        row.put("partnerAmount", kori(rs, "requested_amount"));
        row.put("held", kori(rs, "held_amount"));
        row.put("status", settlementStatus(rs.getString("status")));
        row.put("paidDate", date(rs.getTimestamp("paid_at")));
        return row;
    }

    private MapSqlParameterSource baseParams(long merchantId, String countryScope) {
        return new MapSqlParameterSource()
                .addValue("merchantId", merchantId)
                .addValue("countryScope", countryScope);
    }

    private MapSqlParameterSource periodParams(long merchantId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
        return baseParams(merchantId, countryScope)
                .addValue("periodStart", periodStart)
                .addValue("periodEnd", periodEnd);
    }

    private long countBy(String sql, MapSqlParameterSource params) {
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count == null ? 0L : count;
    }

    private String productStatus(long merchantId) {
        List<String> rows = jdbcTemplate.query("""
                SELECT product_status
                  FROM merchant_store_profiles
                 WHERE merchant_id = :merchantId
                 LIMIT 1
                """, Map.of("merchantId", merchantId), (rs, rowNum) -> valueOrDash(rs.getString("product_status")));
        return rows.stream().findFirst().orElse("-") + "건";
    }

    private static LinkedHashMap<String, Object> kpi(String id, String labelKey, Object value, Object delta, String tag, String accent) {
        LinkedHashMap<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("id", id);
        kpi.put("labelKey", labelKey);
        kpi.put("value", String.valueOf(value));
        if (delta != null) kpi.put("delta", delta);
        if (tag != null) kpi.put("tag", tag);
        kpi.put("accent", accent);
        return kpi;
    }

    private static LinkedHashMap<String, Object> stat(String id, String labelKey, Object value) {
        LinkedHashMap<String, Object> stat = new LinkedHashMap<>();
        stat.put("id", id);
        stat.put("labelKey", labelKey);
        stat.put("value", String.valueOf(value));
        return stat;
    }

    private static LinkedHashMap<String, Object> metric(String id, String labelKey, String value, String noteKey, String chip, boolean chipSolid) {
        LinkedHashMap<String, Object> metric = stat(id, labelKey, value);
        if (noteKey != null) metric.put("noteKey", noteKey);
        metric.put("chip", chip);
        if (chipSolid) metric.put("chipSolid", true);
        return metric;
    }

    private static LinkedHashMap<String, Object> status(String labelKey, String value) {
        LinkedHashMap<String, Object> status = field(labelKey, value);
        status.put("chip", "#1ad1ff");
        return status;
    }

    private static LinkedHashMap<String, Object> field(String labelKey, String value) {
        LinkedHashMap<String, Object> field = new LinkedHashMap<>();
        field.put("labelKey", labelKey);
        field.put("value", valueOrDash(value));
        return field;
    }

    private static LinkedHashMap<String, Object> wideField(String labelKey, String value) {
        LinkedHashMap<String, Object> field = field(labelKey, value);
        field.put("wide", true);
        return field;
    }

    private static String merchantCode(long id) {
        return "MER-" + String.format("%05d", id);
    }

    private static String partnerRef(ResultSet rs) throws SQLException {
        long partnerId = rs.getLong("partner_id");
        return rs.wasNull() ? "-" : "SP-" + String.format("%05d", partnerId);
    }

    private static String merchantStatus(String status) {
        return "MERCHANT_APPROVED".equals(status) ? "승인" : valueOrDash(status);
    }

    private static String storeAccessStatus(String status) {
        return "ALLOWED".equals(status) ? "활성" : valueOrDash(status);
    }

    private static String paymentStatus(String status) {
        return switch (status) {
            case "COMPLETED" -> "완료";
            case "CANCELLED" -> "취소";
            case "REFUNDED" -> "환불";
            case "FAILED" -> "실패";
            default -> valueOrDash(status);
        };
    }

    private static String settlementStatus(String status) {
        return switch (status) {
            case "REQUESTED", "REVIEWING" -> "본사 검토중";
            case "APPROVED" -> "지급 대기";
            case "PAID" -> "지급 완료";
            case "HELD", "ADJUSTMENT_REQUIRED" -> "보류";
            case "REJECTED" -> "거절";
            case "CANCELLED" -> "취소";
            default -> valueOrDash(status);
        };
    }

    private static String noticeAuthor(String senderType) {
        return switch (senderType) {
            case "HQ" -> "본사";
            case "LEADER" -> "리더";
            case "PARTNER" -> "파트너";
            default -> valueOrDash(senderType);
        };
    }

    private static String actionType(String actionType) {
        if (actionType == null) return "-";
        if (actionType.contains("LOGIN")) return "로그인";
        if (actionType.contains("NOTICE")) return "공지";
        if (actionType.contains("SETTLEMENT")) return "정산";
        return actionType;
    }

    private static String actionStatus(String status) {
        return switch (status) {
            case "SUCCESS" -> "성공";
            case "FAILED" -> "실패";
            case "REVIEWING" -> "검토중";
            default -> valueOrDash(status);
        };
    }

    private static long countAction(List<Map<String, Object>> rows, String needle) {
        return rows.stream().filter(row -> String.valueOf(row.get("task")).contains(needle)).count();
    }

    private static long countStatus(List<Map<String, Object>> rows, String status) {
        return rows.stream().filter(row -> status.equals(row.get("status"))).count();
    }

    private static long failCount(List<Map<String, Object>> rows) {
        return rows.stream().filter(row -> List.of("실패", "취소", "환불").contains(row.get("status"))).count();
    }

    private static String sumRows(List<Map<String, Object>> rows, String key) {
        return rows.stream()
                .map(row -> String.valueOf(row.getOrDefault(key, "0")).replace(" KORI", ""))
                .map(BigDecimal::new)
                .reduce(ZERO, BigDecimal::add)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static Timestamp coalesceTimestamp(ResultSet rs, String first, String second) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(first);
        return timestamp == null ? rs.getTimestamp(second) : timestamp;
    }

    private static String valueOrDash(String first, String second) {
        return first == null || first.isBlank() ? valueOrDash(second) : first;
    }

    private static String valueOrDash(String value) {
        return value == null || value.isBlank() || "null".equals(value) ? "-" : value;
    }

    private static String date(Timestamp timestamp) {
        return timestamp == null ? "-" : DATE.format(timestamp.toLocalDateTime().toLocalDate());
    }

    private static String dateTime(Timestamp timestamp) {
        return timestamp == null ? "-" : DATE_TIME.format(timestamp.toLocalDateTime());
    }

    private static String kori(ResultSet rs, String column) throws SQLException {
        return kori(amount(rs, column));
    }

    private static String kori(BigDecimal amount) {
        return amountText(amount) + " KORI";
    }

    private static String amountText(BigDecimal amount) {
        return amount == null ? "0" : amount.stripTrailingZeros().toPlainString();
    }

    private static BigDecimal amount(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? ZERO : value;
    }

    private static BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return ZERO;
    }
}
