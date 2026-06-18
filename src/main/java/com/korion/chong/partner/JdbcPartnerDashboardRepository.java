package com.korion.chong.partner;

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
public class JdbcPartnerDashboardRepository implements PartnerDashboardRepository {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcPartnerDashboardRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<PartnerProfile> findPartnerProfile(long partnerId) {
        List<PartnerProfile> rows = jdbcTemplate.query("""
                SELECT p.id,
                       p.user_id,
                       u.login_id,
                       p.status,
                       COALESCE(p.assigned_country, p.country) AS country_scope,
                       p.region,
                       p.city
                  FROM partners p
                  JOIN users u ON u.id = p.user_id
                 WHERE p.id = :partnerId
                   AND p.partner_type = 'SALES_PARTNER'
                   AND p.status = 'SALES_PARTNER_APPROVED'
                """, Map.of("partnerId", partnerId), (rs, rowNum) -> new PartnerProfile(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("login_id"),
                rs.getString("status"),
                rs.getString("country_scope"),
                rs.getString("region"),
                rs.getString("city")
        ));
        return rows.stream().findFirst();
    }

    @Override
    public Map<String, Object> findDashboard(long partnerId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
        MapSqlParameterSource params = periodParams(partnerId, countryScope, periodStart, periodEnd);
        Map<String, Object> summary = jdbcTemplate.queryForMap("""
                SELECT
                    (SELECT COUNT(*) FROM merchants m
                      WHERE m.parent_sales_partner_id = :partnerId AND m.country = :countryScope) AS merchant_count,
                    (SELECT COUNT(*) FROM merchants m
                      WHERE m.parent_sales_partner_id = :partnerId AND m.country = :countryScope
                        AND m.status <> 'MERCHANT_APPROVED') AS pending_count,
                    (SELECT COUNT(*) FROM notices) AS notice_count,
                    COALESCE((SELECT SUM(kp.amount) FROM korion_payments kp
                      JOIN merchants m ON m.id = kp.merchant_id
                     WHERE kp.sales_partner_id = :partnerId
                       AND m.country = :countryScope
                       AND kp.status = 'COMPLETED'
                       AND kp.paid_at >= :periodStart
                       AND kp.paid_at < :periodEnd), 0) AS monthly_volume,
                    (SELECT COUNT(*) FROM korion_payments kp
                      JOIN merchants m ON m.id = kp.merchant_id
                     WHERE kp.sales_partner_id = :partnerId
                       AND m.country = :countryScope
                       AND kp.status = 'COMPLETED'
                       AND kp.paid_at >= :periodStart
                       AND kp.paid_at < :periodEnd) AS monthly_count,
                    COALESCE((SELECT SUM(pcl.amount) FROM partner_commission_ledger pcl
                     WHERE pcl.partner_id = :partnerId
                       AND pcl.partner_role = 'SALES_PARTNER'
                       AND pcl.status = 'CONFIRMED'
                       AND pcl.created_at >= :periodStart
                       AND pcl.created_at < :periodEnd), 0) AS total_fee
                """, params);

        return Map.of("kpis", List.of(
                kpi("sales-partner", "pdash.kpi.salesPartner", "1", null, "관리", "cyan"),
                kpi("sub-merchant", "pdash.kpi.subMerchant", summary.get("merchant_count"), null, "관리", "green"),
                kpi("pending-approval", "pdash.kpi.pendingApproval", summary.get("pending_count"), null, "리더/본사", "orange"),
                kpi("unread-notice", "pdash.kpi.unreadNotice", summary.get("notice_count") + "건", null, null, "orange"),
                kpi("monthly-volume", "pdash.kpi.monthlyVolume", kori(asBigDecimal(summary.get("monthly_volume"))), null, null, "cyan"),
                kpi("monthly-count", "pdash.kpi.monthlyCount", summary.get("monthly_count"), null, null, "green"),
                kpi("total-fee", "pdash.kpi.totalFee", kori(asBigDecimal(summary.get("total_fee"))), null, "정산가능", "purple"),
                kpi("risk", "pdash.kpi.risk", "0건", null, "주의", "red")
        ));
    }

    @Override
    public Map<String, Object> findMerchantApplications(long partnerId, String countryScope) {
        MapSqlParameterSource params = baseParams(partnerId, countryScope);
        long active = countBy("""
                SELECT COUNT(*) FROM merchants
                 WHERE parent_sales_partner_id = :partnerId
                   AND country = :countryScope
                   AND status = 'MERCHANT_APPROVED'
                """, params);
        long applying = countBy("""
                SELECT COUNT(*) FROM distributor_signup_applications
                 WHERE owner_partner_id = :partnerId
                   AND country = :countryScope
                   AND applicant_type = 'MERCHANT'
                   AND status = 'REQUESTED'
                """, params);
        long pending = countBy("""
                SELECT COUNT(*) FROM distributor_signup_applications
                 WHERE owner_partner_id = :partnerId
                   AND country = :countryScope
                   AND applicant_type = 'MERCHANT'
                   AND status IN ('REVIEWING', 'NEED_MORE_INFO', 'HOLD')
                """, params);
        long rejected = countBy("""
                SELECT COUNT(*) FROM distributor_signup_applications
                 WHERE owner_partner_id = :partnerId
                   AND country = :countryScope
                   AND applicant_type = 'MERCHANT'
                   AND status = 'REJECTED'
                """, params);
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, company_name, contact_name, telegram, region, city,
                       business_type, status, created_at
                  FROM distributor_signup_applications
                 WHERE owner_partner_id = :partnerId
                   AND country = :countryScope
                   AND applicant_type = 'MERCHANT'
                   AND status IN ('REQUESTED', 'REVIEWING', 'NEED_MORE_INFO', 'HOLD')
                 ORDER BY created_at DESC, id DESC
                 LIMIT 50
                """, params, this::merchantApplicationRow);
        return page(List.of(
                stat("active", "preq.kpi.active", active),
                stat("applying", "preq.kpi.applying", applying),
                stat("pending", "preq.kpi.pending", pending),
                stat("black", "preq.kpi.black", rejected)
        ), rows);
    }

    @Override
    public Map<String, Object> findMerchantApplicationDetail(long partnerId, String countryScope) {
        return findProfile(partnerId, countryScope);
    }

    @Override
    public Map<String, Object> findMerchants(long partnerId, String countryScope) {
        MapSqlParameterSource params = baseParams(partnerId, countryScope);
        long total = countBy("""
                SELECT COUNT(*) FROM merchants
                 WHERE parent_sales_partner_id = :partnerId
                   AND country = :countryScope
                """, params);
        long active = countBy("""
                SELECT COUNT(*) FROM merchants
                 WHERE parent_sales_partner_id = :partnerId
                   AND country = :countryScope
                   AND status = 'MERCHANT_APPROVED'
                """, params);
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT m.id, m.merchant_code, m.merchant_name, m.city,
                       COALESCE(SUM(kp.amount), 0) AS volume,
                       COUNT(kp.id) AS tx_count,
                       MAX(kp.paid_at) AS last_tx
                  FROM merchants m
             LEFT JOIN korion_payments kp ON kp.merchant_id = m.id AND kp.status = 'COMPLETED'
                 WHERE m.parent_sales_partner_id = :partnerId
                   AND m.country = :countryScope
                 GROUP BY m.id, m.merchant_code, m.merchant_name, m.city
                 ORDER BY last_tx DESC NULLS LAST, m.id DESC
                 LIMIT 50
                """, params, (rs, rowNum) -> merchantRow(rs, rowNum, partnerId));
        return page(List.of(
                stat("total", "merchantList.kpi.total", total),
                stat("active", "merchantList.kpi.active", active),
                stat("suspended", "merchantList.kpi.suspended", total - active),
                stat("black", "merchantList.kpi.black", 0)
        ), rows);
    }

    @Override
    public Map<String, Object> findMerchantSales(long partnerId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
        List<Map<String, Object>> rows = merchantSalesRows(partnerId, countryScope, periodStart, periodEnd);
        LinkedHashMap<String, Object> result = salesSummary(partnerId, countryScope, periodStart, periodEnd);
        result.put("t1Rows", rows);
        result.put("t2Rows", rows);
        return result;
    }

    @Override
    public Map<String, Object> findSettlementRequestSummary(long partnerId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
        MapSqlParameterSource params = periodParams(partnerId, countryScope, periodStart, periodEnd);
        Map<String, Object> amounts = jdbcTemplate.queryForMap("""
                SELECT COALESCE(SUM(kp.amount), 0) AS target_amount,
                       COALESCE(SUM(kp.total_fee), 0) AS total_fee,
                       COALESCE(SUM(kp.sales_partner_fee), 0) AS partner_fee
                  FROM korion_payments kp
                  JOIN merchants m ON m.id = kp.merchant_id
                 WHERE kp.sales_partner_id = :partnerId
                   AND m.country = :countryScope
                   AND kp.status = 'COMPLETED'
                   AND kp.paid_at >= :periodStart
                   AND kp.paid_at < :periodEnd
                """, params);
        BigDecimal target = asBigDecimal(amounts.get("target_amount"));
        BigDecimal totalFee = asBigDecimal(amounts.get("total_fee"));
        BigDecimal partnerFee = asBigDecimal(amounts.get("partner_fee"));

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("banner", Map.of(
                "notice", "마지막 정산 이후 확정 거래만 기준으로 계산됩니다.",
                "lastDate", DATE.format(periodStart.minusDays(1)),
                "period", DATE.format(periodStart) + " ~ " + DATE.format(periodEnd.minusDays(1)),
                "exclude", DATE.format(periodEnd) + " 이후 거래는 제외",
                "method", "본사 승인 후 자동 정산"
        ));
        result.put("stats", List.of(
                metric("targetAmount", "settle.req.stat.targetAmount", kori(target)),
                metric("totalFee", "settle.req.stat.totalFee", kori(totalFee)),
                metric("subMerchantFee", "psreq.stat.subMerchantFee", kori(partnerFee)),
                metric("held", "settle.req.stat.held", "0 KORI"),
                metric("finalAmount", "settle.req.stat.finalAmount", kori(partnerFee)),
                metric("problemCount", "settle.req.stat.problemCount", "0건")
        ));
        result.put("calc", Map.of("merchantProfit", amountText(partnerFee), "held", "0", "final", amountText(partnerFee), "unit", "KORI", "gauges", List.of()));
        result.put("feeStructure", List.of());
        result.put("merchantTable", Map.of("descKey", "psreq.pt.desc", "rows", List.of()));
        result.put("heldTable", Map.of("descKey", "settle.req.ht.desc", "rows", List.of()));
        result.put("summary", Map.of(
                "period", DATE.format(periodStart) + " ~ " + DATE.format(periodEnd.minusDays(1)),
                "lastDate", DATE.format(periodStart.minusDays(1)),
                "finalAmount", kori(partnerFee),
                "held", "0 KORI",
                "requestAmount", kori(partnerFee),
                "wallet", "-",
                "currency", "KORI",
                "memoPlaceholder", "본사에 전달할 정산 요청 메모를 입력하세요."
        ));
        result.put("checks", List.of("정산 대상 기간 확인", "보류 거래 제외 확인"));
        result.put("form", Map.of("fields", List.of()));
        return result;
    }

    @Override
    public Map<String, Object> findSettlementHistory(long partnerId, String countryScope) {
        MapSqlParameterSource params = baseParams(partnerId, countryScope);
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT sr.id, sr.request_no, sr.period_start, sr.period_end,
                       sr.requested_amount, sr.held_amount, sr.status, sr.requested_at, sr.paid_at,
                       COALESCE(SUM(ce.gross_amount), 0) AS total_amount
                  FROM distributor_settlement_requests sr
             LEFT JOIN distributor_commission_entries ce ON ce.settlement_request_id = sr.id
                 WHERE sr.recipient_type = 'PARTNER'
                   AND sr.recipient_partner_id = :partnerId
                 GROUP BY sr.id, sr.request_no, sr.period_start, sr.period_end,
                          sr.requested_amount, sr.held_amount, sr.status, sr.requested_at, sr.paid_at
                 ORDER BY sr.requested_at DESC, sr.id DESC
                 LIMIT 50
                """, params, this::settlementHistoryRow);
        return Map.of(
                "lastSettleDate", lastPaidSettlementDate(params),
                "thisRequestAmount", kori(pendingSettlementAmount(params)),
                "rows", rows
        );
    }

    @Override
    public Map<String, Object> findSettlementDetail(long partnerId, String countryScope) {
        MapSqlParameterSource params = baseParams(partnerId, countryScope);
        List<Map<String, Object>> details = jdbcTemplate.query("""
                SELECT sr.id, sr.request_no, sr.period_start, sr.period_end,
                       sr.requested_amount, sr.held_amount, sr.status, sr.requested_at, sr.paid_at,
                       COALESCE(SUM(ce.gross_amount), 0) AS total_amount,
                       COALESCE(SUM(ce.fee_amount), 0) AS total_fee,
                       COALESCE(SUM(ce.commission_amount), 0) AS partner_amount
                  FROM distributor_settlement_requests sr
             LEFT JOIN distributor_commission_entries ce ON ce.settlement_request_id = sr.id
                 WHERE sr.recipient_type = 'PARTNER'
                   AND sr.recipient_partner_id = :partnerId
                 GROUP BY sr.id, sr.request_no, sr.period_start, sr.period_end,
                          sr.requested_amount, sr.held_amount, sr.status, sr.requested_at, sr.paid_at
                 ORDER BY sr.requested_at DESC, sr.id DESC
                 LIMIT 1
                """, params, (rs, rowNum) -> partnerSettlementDetail(rs, partnerId));
        if (!details.isEmpty()) {
            Map<String, Object> detail = details.get(0);
            long settlementRequestId = ((Number) detail.get("settlementRequestId")).longValue();
            detail.remove("settlementRequestId");
            detail.put("merchantRows", settlementMerchantRows(settlementRequestId));
            detail.put("heldRows", settlementHeldRows(settlementRequestId));
            return detail;
        }
        return Map.of(
                "no", "-",
                "status", "내역 없음",
                "basicInfo", List.of(),
                "amountSummary", List.of(),
                "merchantRows", List.of(),
                "heldRows", List.of()
        );
    }

    @Override
    public Map<String, Object> findHqNotices(long partnerId, String countryScope) {
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, title, created_at, is_important
                  FROM notices
                 ORDER BY created_at DESC, id DESC
                 LIMIT 50
                """, Map.of(), (rs, rowNum) -> {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("no", "N-" + rs.getLong("id"));
            row.put("author", "본사");
            row.put("target", "파트너");
            row.put("title", rs.getString("title"));
            row.put("sentDate", date(rs.getTimestamp("created_at")));
            row.put("read", rs.getBoolean("is_important") ? "확인필요" : "읽음");
            return row;
        });
        return Map.of("rows", rows);
    }

    @Override
    public Map<String, Object> findNoticeSendSummary(long partnerId, String countryScope) {
        long merchantCount = countBy("""
                SELECT COUNT(*) FROM merchants
                 WHERE parent_sales_partner_id = :partnerId
                   AND country = :countryScope
                """, baseParams(partnerId, countryScope));
        return Map.of("metrics", List.of(
                metric("merchant", "notice.send.kpi.merchant", merchantCount + "개"),
                metric("today", "notice.send.kpi.today", "0건"),
                metric("scheduled", "notice.send.kpi.scheduled", "0건")
        ));
    }

    @Override
    public Map<String, Object> findNoticeHistory(long partnerId, String countryScope) {
        return Map.of(
                "metrics", List.of(
                        metric("total", "notice.hist.kpi.total", "0건"),
                        metric("month", "notice.hist.kpi.month", "0건"),
                        metric("scheduled", "notice.hist.kpi.scheduled", "0건"),
                        metric("done", "notice.hist.kpi.done", "0건"),
                        metric("failed", "notice.hist.kpi.failed", "0건")
                ),
                "rows", List.of()
        );
    }

    @Override
    public Map<String, Object> findProfile(long partnerId, String countryScope) {
        return jdbcTemplate.queryForObject("""
                SELECT p.id, p.status, COALESCE(p.assigned_country, p.country) AS country,
                       p.region, p.city, p.created_at, p.approved_at,
                       u.login_id, u.name, u.nickname, u.phone,
                       COUNT(DISTINCT m.id) AS merchant_count,
                       COALESCE(SUM(pcl.amount), 0) AS unpaid_fee
                  FROM partners p
                  JOIN users u ON u.id = p.user_id
             LEFT JOIN merchants m ON m.parent_sales_partner_id = p.id
             LEFT JOIN partner_commission_ledger pcl ON pcl.partner_id = p.id AND pcl.status = 'CONFIRMED'
                 WHERE p.id = :partnerId
                   AND COALESCE(p.assigned_country, p.country) = :countryScope
                 GROUP BY p.id, p.status, COALESCE(p.assigned_country, p.country), p.region, p.city, p.created_at, p.approved_at,
                          u.login_id, u.name, u.nickname, u.phone
                """, baseParams(partnerId, countryScope), (rs, rowNum) -> {
            String code = partnerCode(rs.getLong("id"));
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
            result.put("statusItems", List.of(
                    status("profile.status.leaderCode", code),
                    status("profile.status.country", rs.getString("country")),
                    status("profile.status.city", valueOrDash(rs.getString("city"), rs.getString("region"))),
                    status("profile.status.approval", partnerStatus(rs.getString("status"))),
                    status("profile.status.operation", "활성"),
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
                    field("profile.acc.wallet", "-"),
                    field("profile.acc.lastActive", "-")
            ));
            result.put("basicFields", List.of(
                    field("profile.basic.leaderCode", code),
                    field("profile.basic.country", rs.getString("country")),
                    field("profile.basic.region", valueOrDash(rs.getString("region"))),
                    field("profile.basic.language", "-"),
                    field("profile.basic.merchantCount", String.valueOf(rs.getLong("merchant_count"))),
                    field("profile.basic.unpaidFee", kori(rs, "unpaid_fee"))
            ));
            return result;
        });
    }

    @Override
    public Map<String, Object> findActivityLogs(long partnerId, String countryScope) {
        return Map.of(
                "metrics", List.of(
                        metric("recent", "act.kpi.recent", "-"),
                        metric("visits", "act.kpi.visits", "0회"),
                        metric("notices", "act.kpi.notices", "0건"),
                        metric("failed", "act.kpi.failed", "0건")
                ),
                "rows", List.of()
        );
    }

    private List<Map<String, Object>> merchantSalesRows(long partnerId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
        return jdbcTemplate.query("""
                SELECT m.id, m.merchant_code, m.merchant_name, m.city,
                       COALESCE(SUM(kp.amount), 0) AS month_revenue,
                       COUNT(kp.id) AS month_count,
                       COALESCE(SUM(kp.total_fee), 0) AS fee,
                       MAX(kp.paid_at) AS recent_pay
                  FROM merchants m
             LEFT JOIN korion_payments kp ON kp.merchant_id = m.id
                   AND kp.sales_partner_id = :partnerId
                   AND kp.paid_at >= :periodStart
                   AND kp.paid_at < :periodEnd
                   AND kp.status = 'COMPLETED'
                 WHERE m.parent_sales_partner_id = :partnerId
                   AND m.country = :countryScope
                 GROUP BY m.id, m.merchant_code, m.merchant_name, m.city
                 ORDER BY month_revenue DESC, m.id DESC
                 LIMIT 50
                """, periodParams(partnerId, countryScope, periodStart, periodEnd), (rs, rowNum) -> {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("no", String.valueOf(rowNum + 1));
            row.put("partner", partnerCode(partnerId));
            row.put("code", merchantCode(rs.getLong("id")));
            row.put("merchantCode", valueOrDash(rs.getString("merchant_code"), merchantCode(rs.getLong("id"))));
            row.put("name", rs.getString("merchant_name"));
            row.put("merchantName", rs.getString("merchant_name"));
            row.put("telegram", "-");
            row.put("region", valueOrDash(rs.getString("city")));
            row.put("monthRevenue", kori(rs, "month_revenue"));
            row.put("amount", kori(rs, "month_revenue"));
            row.put("monthCount", String.valueOf(rs.getLong("month_count")));
            row.put("recentPay", kori(rs, "month_revenue"));
            row.put("fee", amountText(amount(rs, "fee")));
            row.put("unsettledFee", kori(rs, "fee"));
            row.put("recentActivity", date(rs.getTimestamp("recent_pay")));
            row.put("recentPay2", date(rs.getTimestamp("recent_pay")));
            row.put("qrUsage", "-");
            return row;
        });
    }

    private Map<String, Object> merchantRow(ResultSet rs, int rowNum, long partnerId) throws SQLException {
        BigDecimal volume = amount(rs, "volume");
        long count = rs.getLong("tx_count");
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("no", String.valueOf(rowNum + 1));
        row.put("city", valueOrDash(rs.getString("city")));
        row.put("partner", partnerCode(partnerId));
        row.put("merchantCode", valueOrDash(rs.getString("merchant_code"), merchantCode(rs.getLong("id"))));
        row.put("name", rs.getString("merchant_name"));
        row.put("volume", kori(volume));
        row.put("txCount", String.valueOf(count));
        row.put("avgPay", count == 0 ? "0 KORI" : kori(volume.divide(BigDecimal.valueOf(count), java.math.RoundingMode.HALF_UP)));
        row.put("qrUsage", "-");
        row.put("lastTx", dateTime(rs.getTimestamp("last_tx")));
        row.put("actions", List.of("상세", "재검토요청"));
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

    private Map<String, Object> partnerSettlementDetail(ResultSet rs, long partnerId) throws SQLException {
        String requestNo = rs.getString("request_no");
        String status = settlementStatus(rs.getString("status"));
        String period = DATE.format(rs.getDate("period_start").toLocalDate()) + " ~ " + DATE.format(rs.getDate("period_end").toLocalDate());
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("settlementRequestId", rs.getLong("id"));
        result.put("no", requestNo);
        result.put("status", status);
        result.put("basicInfo", List.of(
                field("pdetail.a.no", requestNo),
                field("pdetail.a.name", partnerCode(partnerId)),
                field("pdetail.a.code", partnerCode(partnerId)),
                field("pdetail.a.period", period),
                field("pdetail.a.lastDate", DATE.format(rs.getDate("period_end").toLocalDate())),
                highlightedField("pdetail.a.appliedDate", date(rs.getTimestamp("requested_at"))),
                field("pdetail.a.paidDate", date(rs.getTimestamp("paid_at"))),
                field("pdetail.a.status", status)
        ));
        result.put("amountSummary", List.of(
                field("pdetail.b.total", kori(rs, "total_amount")),
                field("pdetail.b.totalFee", kori(rs, "total_fee")),
                field("pdetail.b.merchantProfit", kori(rs, "partner_amount")),
                field("pdetail.b.held", kori(rs, "held_amount")),
                highlightedField("pdetail.b.finalAmount", kori(rs, "requested_amount"))
        ));
        return result;
    }

    private List<Map<String, Object>> settlementMerchantRows(long settlementRequestId) {
        return jdbcTemplate.query("""
                SELECT m.id, m.merchant_code, m.merchant_name,
                       COALESCE(SUM(ce.gross_amount), 0) AS gross_amount,
                       COALESCE(SUM(ce.commission_amount), 0) AS fee,
                       MAX(sr.paid_at) AS paid_at,
                       MAX(ce.settlement_status) AS status
                  FROM distributor_commission_entries ce
                  JOIN merchants m ON m.id = ce.merchant_id
                  JOIN distributor_settlement_requests sr ON sr.id = ce.settlement_request_id
                 WHERE ce.settlement_request_id = :settlementRequestId
                   AND ce.beneficiary_type = 'PARTNER'
                 GROUP BY m.id, m.merchant_code, m.merchant_name
                 ORDER BY fee DESC, m.id DESC
                 LIMIT 50
                """, Map.of("settlementRequestId", settlementRequestId), (rs, rowNum) -> {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("name", rs.getString("merchant_name"));
            row.put("code", valueOrDash(rs.getString("merchant_code"), merchantCode(rs.getLong("id"))));
            row.put("amount", amountText(amount(rs, "gross_amount")));
            row.put("fee", amountText(amount(rs, "fee")));
            row.put("status", settlementStatus(rs.getString("status")));
            row.put("paidDate", date(rs.getTimestamp("paid_at")));
            return row;
        });
    }

    private List<Map<String, Object>> settlementHeldRows(long settlementRequestId) {
        return jdbcTemplate.query("""
                SELECT ce.id, m.merchant_name, p.id AS partner_id, ce.hold_reason,
                       ce.gross_amount, ce.commission_amount, ce.settlement_status
                  FROM distributor_commission_entries ce
                  JOIN merchants m ON m.id = ce.merchant_id
             LEFT JOIN partners p ON p.id = m.parent_sales_partner_id
                 WHERE ce.settlement_request_id = :settlementRequestId
                   AND ce.settlement_status IN ('HELD', 'REJECTED', 'CANCELLED')
                 ORDER BY ce.id DESC
                 LIMIT 50
                """, Map.of("settlementRequestId", settlementRequestId), (rs, rowNum) -> {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("txNo", "CE-" + rs.getLong("id"));
            row.put("merchant", rs.getString("merchant_name"));
            long partnerId = rs.getLong("partner_id");
            row.put("partner", rs.wasNull() ? "-" : partnerCode(partnerId));
            row.put("reason", valueOrDash(rs.getString("hold_reason"), settlementStatus(rs.getString("settlement_status"))));
            row.put("amount", amountText(amount(rs, "gross_amount")));
            row.put("heldFee", amountText(amount(rs, "commission_amount")));
            row.put("status", settlementStatus(rs.getString("settlement_status")));
            return row;
        });
    }

    private String lastPaidSettlementDate(MapSqlParameterSource params) {
        List<String> rows = jdbcTemplate.query("""
                SELECT paid_at
                  FROM distributor_settlement_requests
                 WHERE recipient_type = 'PARTNER'
                   AND recipient_partner_id = :partnerId
                   AND status = 'PAID'
                   AND paid_at IS NOT NULL
                 ORDER BY paid_at DESC
                 LIMIT 1
                """, params, (rs, rowNum) -> date(rs.getTimestamp("paid_at")));
        return rows.stream().findFirst().orElse("-");
    }

    private BigDecimal pendingSettlementAmount(MapSqlParameterSource params) {
        BigDecimal amount = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(requested_amount), 0)
                  FROM distributor_settlement_requests
                 WHERE recipient_type = 'PARTNER'
                   AND recipient_partner_id = :partnerId
                   AND status IN ('DRAFT', 'REQUESTED', 'REVIEWING', 'APPROVED', 'HELD', 'ADJUSTMENT_REQUIRED')
                """, params, BigDecimal.class);
        return amount == null ? ZERO : amount;
    }

    private Map<String, Object> merchantApplicationRow(ResultSet rs, int rowNum) throws SQLException {
        long id = rs.getLong("id");
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("no", String.valueOf(rowNum + 1));
        row.put("applicationId", id);
        row.put("code", "REQ-" + String.format("%05d", id));
        row.put("name", rs.getString("company_name"));
        row.put("merchantName", rs.getString("company_name"));
        row.put("contactName", rs.getString("contact_name"));
        row.put("telegram", valueOrDash(rs.getString("telegram")));
        row.put("region", valueOrDash(rs.getString("city"), rs.getString("region")));
        row.put("industry", valueOrDash(rs.getString("business_type")));
        row.put("opStatus", applicationStatus(rs.getString("status")));
        row.put("status", rs.getString("status"));
        row.put("date", date(rs.getTimestamp("created_at")));
        return row;
    }

    private LinkedHashMap<String, Object> salesSummary(long partnerId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
        Map<String, Object> summary = jdbcTemplate.queryForMap("""
                SELECT COALESCE(SUM(kp.amount), 0) AS month_revenue,
                       COUNT(kp.id) AS month_count,
                       COALESCE(SUM(kp.total_fee), 0) AS month_fee,
                       COALESCE(SUM(kp.sales_partner_fee), 0) AS unsettled_fee
                  FROM korion_payments kp
                  JOIN merchants m ON m.id = kp.merchant_id
                 WHERE kp.sales_partner_id = :partnerId
                   AND m.country = :countryScope
                   AND kp.status = 'COMPLETED'
                   AND kp.paid_at >= :periodStart
                   AND kp.paid_at < :periodEnd
                """, periodParams(partnerId, countryScope, periodStart, periodEnd));
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("stats", List.of(
                stat("todayVolume", "partnerSales.stat.todayVolume", "0 KORI"),
                stat("todayCount", "partnerSales.stat.todayCount", "0"),
                stat("monthRevenue", "partnerSales.stat.monthRevenue", kori(asBigDecimal(summary.get("month_revenue")))),
                stat("monthCount", "partnerSales.stat.monthCount", summary.get("month_count")),
                stat("monthFee", "partnerSales.stat.monthFee", kori(asBigDecimal(summary.get("month_fee")))),
                stat("unsettledFee", "partnerSales.stat.unsettledFee", kori(asBigDecimal(summary.get("unsettled_fee")))),
                stat("offlineRatio", "partnerSales.stat.offlineRatio", "0/0/0"),
                stat("failCancel", "partnerSales.stat.failCancel", "0")
        ));
        return result;
    }

    private MapSqlParameterSource baseParams(long partnerId, String countryScope) {
        return new MapSqlParameterSource()
                .addValue("partnerId", partnerId)
                .addValue("countryScope", countryScope);
    }

    private MapSqlParameterSource periodParams(long partnerId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
        return baseParams(partnerId, countryScope)
                .addValue("periodStart", periodStart)
                .addValue("periodEnd", periodEnd);
    }

    private long countBy(String sql, MapSqlParameterSource params) {
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count == null ? 0L : count;
    }

    private LinkedHashMap<String, Object> page(List<Map<String, Object>> stats, List<Map<String, Object>> rows) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("stats", stats);
        result.put("rows", rows);
        return result;
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

    private static LinkedHashMap<String, Object> metric(String id, String labelKey, String value) {
        LinkedHashMap<String, Object> metric = stat(id, labelKey, value);
        metric.put("chip", "#24e6b8");
        return metric;
    }

    private static LinkedHashMap<String, Object> status(String labelKey, String value) {
        LinkedHashMap<String, Object> status = new LinkedHashMap<>();
        status.put("labelKey", labelKey);
        status.put("value", valueOrDash(value));
        status.put("chip", "#1ad1ff");
        return status;
    }

    private static LinkedHashMap<String, Object> field(String labelKey, String value) {
        LinkedHashMap<String, Object> field = new LinkedHashMap<>();
        field.put("labelKey", labelKey);
        field.put("value", valueOrDash(value));
        return field;
    }

    private static LinkedHashMap<String, Object> highlightedField(String labelKey, String value) {
        LinkedHashMap<String, Object> field = field(labelKey, value);
        field.put("color", "#24e6b8");
        return field;
    }

    private static String partnerCode(long id) {
        return "SP-" + String.format("%05d", id);
    }

    private static String merchantCode(long id) {
        return "MER-" + String.format("%05d", id);
    }

    private static String partnerStatus(String status) {
        return "SALES_PARTNER_APPROVED".equals(status) ? "승인" : valueOrDash(status);
    }

    private static String settlementStatus(String status) {
        return switch (status) {
            case "DRAFT" -> "임시저장";
            case "REQUESTED", "REVIEWING" -> "본사 검토중";
            case "APPROVED" -> "지급 대기";
            case "PAID" -> "지급 완료";
            case "HELD", "ADJUSTMENT_REQUIRED" -> "보류";
            case "REJECTED" -> "거절";
            case "CANCELLED" -> "취소";
            default -> valueOrDash(status);
        };
    }

    private static String applicationStatus(String status) {
        return switch (status) {
            case "REQUESTED" -> "승인신청";
            case "REVIEWING" -> "검토중";
            case "NEED_MORE_INFO" -> "자료요청";
            case "HOLD" -> "보류";
            case "REJECTED" -> "거절";
            case "APPROVED" -> "승인";
            default -> valueOrDash(status);
        };
    }

    private static String valueOrDash(String first, String second) {
        return first == null || first.isBlank() ? valueOrDash(second) : first;
    }

    private static String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
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
