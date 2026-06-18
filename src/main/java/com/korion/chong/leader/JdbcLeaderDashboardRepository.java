package com.korion.chong.leader;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLeaderDashboardRepository implements LeaderDashboardRepository {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcLeaderDashboardRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<LeaderProfile> findLeaderProfile(long leaderId) {
        String sql = """
                SELECT p.id,
                       p.user_id,
                       u.login_id,
                       p.status,
                       COALESCE(p.assigned_country, p.country) AS country_scope
                  FROM partners p
                  JOIN users u ON u.id = p.user_id
                 WHERE p.id = :leaderId
                   AND p.partner_type = 'COUNTRY_LEADER'
                   AND p.status = 'COUNTRY_LEADER_APPROVED'
                """;
        List<LeaderProfile> rows = jdbcTemplate.query(sql, Map.of("leaderId", leaderId), (rs, rowNum) -> new LeaderProfile(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("login_id"),
                rs.getString("status"),
                rs.getString("country_scope") == null ? List.of() : List.of(rs.getString("country_scope"))
        ));
        return rows.stream().findFirst();
    }

    @Override
    public LeaderDashboardResponse.Kpis findKpis(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
        MapSqlParameterSource params = baseParams(leaderId, countryScope, periodStart, periodEnd);
        return jdbcTemplate.queryForObject("""
                SELECT
                    (SELECT COUNT(*)
                       FROM partners sp
                      WHERE sp.parent_partner_id = :leaderId
                        AND sp.partner_type = 'SALES_PARTNER'
                        AND sp.status = 'SALES_PARTNER_APPROVED'
                        AND COALESCE(sp.assigned_country, sp.country) = :countryScope) AS approved_partner_count,
                    (SELECT COUNT(*)
                       FROM merchants m
                      WHERE m.parent_country_master_id = :leaderId
                        AND m.status = 'MERCHANT_APPROVED'
                        AND m.country = :countryScope) AS approved_merchant_count,
                    COALESCE((SELECT SUM(kp.amount)
                                FROM korion_payments kp
                               WHERE kp.country_master_id = :leaderId
                                 AND kp.status = 'COMPLETED'
                                 AND kp.paid_at >= :periodStart
                                 AND kp.paid_at < :periodEnd), 0) AS completed_transaction_amount,
                    COALESCE((SELECT SUM(pcl.amount)
                                FROM partner_commission_ledger pcl
                               WHERE pcl.partner_id = :leaderId
                                 AND pcl.partner_role = 'COUNTRY_LEADER'
                                 AND pcl.status = 'CONFIRMED'
                                 AND pcl.created_at >= :periodStart
                                 AND pcl.created_at < :periodEnd), 0) AS confirmed_commission_amount
                """, params, (rs, rowNum) -> new LeaderDashboardResponse.Kpis(
                rs.getLong("approved_partner_count"),
                rs.getLong("approved_merchant_count"),
                amount(rs, "completed_transaction_amount"),
                amount(rs, "confirmed_commission_amount")
        ));
    }

    @Override
    public List<LeaderDashboardResponse.MonthlyVolume> findMonthlyVolume(
            long leaderId,
            String countryScope,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        return jdbcTemplate.query("""
                SELECT date_trunc('month', paid_at)::date AS month,
                       COALESCE(SUM(amount), 0) AS amount,
                       COUNT(*) AS transaction_count
                  FROM korion_payments
                 WHERE country_master_id = :leaderId
                   AND status = 'COMPLETED'
                   AND paid_at >= :periodStart
                   AND paid_at < :periodEnd
                 GROUP BY date_trunc('month', paid_at)::date
                 ORDER BY month
                """, baseParams(leaderId, countryScope, periodStart, periodEnd), (rs, rowNum) -> new LeaderDashboardResponse.MonthlyVolume(
                YearMonth.from(rs.getDate("month").toLocalDate()),
                amount(rs, "amount"),
                rs.getLong("transaction_count")
        ));
    }

    @Override
    public LeaderDashboardResponse.FeeSummary findFeeSummary(
            long leaderId,
            String countryScope,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(country_master_fee), 0) AS country_leader_fee,
                       COALESCE(SUM(sales_partner_fee), 0) AS sales_partner_fee,
                       COALESCE(SUM(korion_fee), 0) AS korion_fee
                  FROM korion_payments
                 WHERE country_master_id = :leaderId
                   AND status = 'COMPLETED'
                   AND paid_at >= :periodStart
                   AND paid_at < :periodEnd
                """, baseParams(leaderId, countryScope, periodStart, periodEnd), (rs, rowNum) -> new LeaderDashboardResponse.FeeSummary(
                amount(rs, "country_leader_fee"),
                amount(rs, "sales_partner_fee"),
                amount(rs, "korion_fee")
        ));
    }

    @Override
    public List<LeaderPartnerResponse.PartnerSummary> findPartners(
            long leaderId,
            String countryScope,
            PartnerSearchCriteria criteria
    ) {
        MapSqlParameterSource params = partnerParams(leaderId, countryScope, criteria);
        params.addValue("limit", criteria.size());
        params.addValue("offset", criteria.offset());
        return jdbcTemplate.query("""
                SELECT sp.id,
                       sp.user_id,
                       u.login_id,
                       COALESCE(sp.assigned_country, sp.country) AS country,
                       sp.region,
                       sp.city,
                       sp.status,
                       COUNT(DISTINCT m.id) AS merchant_count,
                       COALESCE(SUM(kp.amount), 0) AS completed_transaction_amount,
                       MAX(COALESCE(kp.paid_at, sp.updated_at, sp.created_at)) AS last_activity_at
                  FROM partners sp
                  JOIN users u ON u.id = sp.user_id
                 LEFT JOIN merchants m ON m.parent_sales_partner_id = sp.id
                 LEFT JOIN korion_payments kp ON kp.sales_partner_id = sp.id AND kp.status = 'COMPLETED'
                 WHERE sp.parent_partner_id = :leaderId
                   AND sp.partner_type = 'SALES_PARTNER'
                   AND sp.status = 'SALES_PARTNER_APPROVED'
                   AND COALESCE(sp.assigned_country, sp.country) = :countryScope
                   AND (:status IS NULL OR :status = 'SALES_PARTNER_APPROVED')
                   AND (:region IS NULL OR sp.region = :region)
                   AND (:keyword IS NULL OR lower(u.login_id) LIKE :keyword OR lower(sp.city) LIKE :keyword)
                 GROUP BY sp.id, sp.user_id, u.login_id, COALESCE(sp.assigned_country, sp.country), sp.region, sp.city, sp.status
                 ORDER BY last_activity_at DESC NULLS LAST, sp.id DESC
                 LIMIT :limit OFFSET :offset
                """, params, partnerMapper());
    }

    @Override
    public long countPartners(long leaderId, String countryScope, PartnerSearchCriteria criteria) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM partners sp
                 JOIN users u ON u.id = sp.user_id
                 WHERE sp.parent_partner_id = :leaderId
                   AND sp.partner_type = 'SALES_PARTNER'
                   AND sp.status = 'SALES_PARTNER_APPROVED'
                   AND COALESCE(sp.assigned_country, sp.country) = :countryScope
                   AND (:status IS NULL OR :status = 'SALES_PARTNER_APPROVED')
                   AND (:region IS NULL OR sp.region = :region)
                   AND (:keyword IS NULL OR lower(u.login_id) LIKE :keyword OR lower(sp.city) LIKE :keyword)
                """, partnerParams(leaderId, countryScope, criteria), Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public Map<String, Object> findSignupApplications(long leaderId, String countryScope, String applicantType) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("leaderId", leaderId)
                .addValue("countryScope", countryScope)
                .addValue("applicantType", applicantType);
        long approvedCount = countBy("""
                SELECT COUNT(*) FROM distributor_signup_applications
                 WHERE applicant_type = :applicantType
                   AND country = :countryScope
                   AND status = 'APPROVED'
                """, params);
        long requestedCount = countBy("""
                SELECT COUNT(*) FROM distributor_signup_applications
                 WHERE applicant_type = :applicantType
                   AND country = :countryScope
                   AND status IN ('REQUESTED', 'REVIEWING', 'NEED_MORE_INFO', 'HOLD')
                """, params);
        long rejectedCount = countBy("""
                SELECT COUNT(*) FROM distributor_signup_applications
                 WHERE applicant_type = :applicantType
                   AND country = :countryScope
                   AND status IN ('REJECTED', 'CANCELLED')
                """, params);

        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, login_id, company_name, telegram, region, city, business_type, status, created_at
                  FROM distributor_signup_applications
                 WHERE applicant_type = :applicantType
                   AND country = :countryScope
                 ORDER BY created_at DESC, id DESC
                 LIMIT 50
                """, params, (rs, rowNum) -> {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("no", String.valueOf(rowNum + 1));
            row.put("code", applicationCode(applicantType, rs.getLong("id")));
            row.put("name", rs.getString("company_name"));
            if ("PARTNER".equals(applicantType)) {
                row.put("region", valueOrDash(rs.getString("region")));
                row.put("subCount", "0");
                row.put("volume", "0 KORI");
                row.put("txCount", "0");
                row.put("hqStatus", signupStatus(rs.getString("status")));
                row.put("opStatus", signupOperationStatus(rs.getString("status")));
            } else {
                row.put("telegram", valueOrDash(rs.getString("telegram")));
                row.put("region", valueOrDash(rs.getString("city"), rs.getString("region")));
                row.put("industry", valueOrDash(rs.getString("business_type")));
                row.put("opStatus", signupOperationStatus(rs.getString("status")));
            }
            row.put("date", date(rs.getTimestamp("created_at")));
            return row;
        });

        return page(
                List.of(
                        stat("active", applicantType.equals("PARTNER") ? "partner.kpi.active" : "merchant.kpi.active", approvedCount),
                        stat(applicantType.equals("PARTNER") ? "requesting" : "recommended",
                                applicantType.equals("PARTNER") ? "partner.kpi.requesting" : "merchant.kpi.recommended",
                                requestedCount),
                        stat(applicantType.equals("PARTNER") ? "waiting" : "direct",
                                applicantType.equals("PARTNER") ? "partner.kpi.waiting" : "merchant.kpi.direct",
                                requestedCount),
                        stat("black", applicantType.equals("PARTNER") ? "partner.kpi.black" : "merchant.kpi.black", rejectedCount)
                ),
                rows
        );
    }

    @Override
    public Map<String, Object> findPartnerSales(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
        MapSqlParameterSource params = baseParams(leaderId, countryScope, periodStart, periodEnd);
        List<Map<String, Object>> partnerRows = jdbcTemplate.query("""
                SELECT sp.id, u.login_id, sp.region, sp.city,
                       COUNT(DISTINCT m.id) AS merchant_count,
                       COALESCE(SUM(kp.amount), 0) AS month_revenue,
                       COUNT(kp.id) AS month_count,
                       COALESCE(SUM(kp.sales_partner_fee), 0) AS unsettled_fee,
                       MAX(kp.paid_at) AS recent_activity
                  FROM partners sp
                  JOIN users u ON u.id = sp.user_id
             LEFT JOIN merchants m ON m.parent_sales_partner_id = sp.id
             LEFT JOIN korion_payments kp ON kp.sales_partner_id = sp.id
                   AND kp.paid_at >= :periodStart
                   AND kp.paid_at < :periodEnd
                   AND kp.status = 'COMPLETED'
                 WHERE sp.parent_partner_id = :leaderId
                   AND sp.partner_type = 'SALES_PARTNER'
                   AND COALESCE(sp.assigned_country, sp.country) = :countryScope
                 GROUP BY sp.id, u.login_id, sp.region, sp.city
                 ORDER BY month_revenue DESC, sp.id DESC
                 LIMIT 50
                """, params, (rs, rowNum) -> {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("no", String.valueOf(rowNum + 1));
            row.put("code", partnerCode(rs.getLong("id")));
            row.put("name", rs.getString("login_id"));
            row.put("telegram", "-");
            row.put("region", valueOrDash(rs.getString("city"), rs.getString("region")));
            row.put("subCount", String.valueOf(rs.getLong("merchant_count")));
            row.put("monthRevenue", kori(rs, "month_revenue"));
            row.put("monthCount", String.valueOf(rs.getLong("month_count")));
            row.put("unsettledFee", kori(rs, "unsettled_fee"));
            row.put("recentActivity", date(rs.getTimestamp("recent_activity")));
            return row;
        });
        List<Map<String, Object>> merchantRows = merchantSalesRows(leaderId, countryScope, periodStart, periodEnd);
        return salesPage(partnerRows, merchantRows, periodStart, periodEnd);
    }

    @Override
    public Map<String, Object> findMerchants(long leaderId, String countryScope) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("leaderId", leaderId)
                .addValue("countryScope", countryScope);
        long total = countBy("""
                SELECT COUNT(*) FROM merchants
                 WHERE parent_country_master_id = :leaderId
                   AND country = :countryScope
                """, params);
        long active = countBy("""
                SELECT COUNT(*) FROM merchants
                 WHERE parent_country_master_id = :leaderId
                   AND country = :countryScope
                   AND status = 'MERCHANT_APPROVED'
                """, params);
        long suspended = countBy("""
                SELECT COUNT(*) FROM merchants
                 WHERE parent_country_master_id = :leaderId
                   AND country = :countryScope
                   AND status <> 'MERCHANT_APPROVED'
                """, params);
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT m.id, m.merchant_code, m.merchant_name, m.city, m.status,
                       COALESCE(sp.id, 0) AS partner_id,
                       COALESCE(SUM(kp.amount), 0) AS volume,
                       COUNT(kp.id) AS tx_count,
                       MAX(kp.paid_at) AS last_tx
                  FROM merchants m
             LEFT JOIN partners sp ON sp.id = m.parent_sales_partner_id
             LEFT JOIN korion_payments kp ON kp.merchant_id = m.id AND kp.status = 'COMPLETED'
                 WHERE m.parent_country_master_id = :leaderId
                   AND m.country = :countryScope
                 GROUP BY m.id, m.merchant_code, m.merchant_name, m.city, m.status, sp.id
                 ORDER BY last_tx DESC NULLS LAST, m.id DESC
                 LIMIT 50
                """, params, (rs, rowNum) -> {
            BigDecimal volume = amount(rs, "volume");
            long count = rs.getLong("tx_count");
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("no", String.valueOf(rowNum + 1));
            row.put("city", valueOrDash(rs.getString("city")));
            row.put("partner", rs.getLong("partner_id") > 0 ? partnerCode(rs.getLong("partner_id")) : "직접 계약");
            row.put("merchantCode", valueOrDash(rs.getString("merchant_code"), merchantCode(rs.getLong("id"))));
            row.put("name", rs.getString("merchant_name"));
            row.put("volume", kori(volume));
            row.put("txCount", String.valueOf(count));
            row.put("avgPay", count == 0 ? "0 KORI" : kori(volume.divide(BigDecimal.valueOf(count), java.math.RoundingMode.HALF_UP)));
            row.put("qrUsage", "-");
            row.put("lastTx", dateTime(rs.getTimestamp("last_tx")));
            row.put("actions", List.of("상세", "재검토요청"));
            return row;
        });
        return page(List.of(
                stat("total", "merchantList.kpi.total", total),
                stat("active", "merchantList.kpi.active", active),
                stat("suspended", "merchantList.kpi.suspended", suspended),
                stat("black", "merchantList.kpi.black", 0)
        ), rows);
    }

    @Override
    public Map<String, Object> findMerchantSales(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
        List<Map<String, Object>> rows = merchantSalesRows(leaderId, countryScope, periodStart, periodEnd);
        LinkedHashMap<String, Object> result = salesSummary(periodStart, periodEnd);
        result.put("t1Rows", rows);
        result.put("t2Rows", rows);
        return result;
    }

    @Override
    public Map<String, Object> findTransactions(long leaderId, String countryScope, String variant, LocalDate periodStart, LocalDate periodEnd) {
        MapSqlParameterSource params = baseParams(leaderId, countryScope, periodStart, periodEnd);
        List<Map<String, Object>> allRows = jdbcTemplate.query("""
                SELECT kp.id, kp.amount, kp.total_fee, kp.net_amount_to_merchant, kp.status, kp.paid_at,
                       m.merchant_code, m.merchant_name, sp.id AS partner_id
                  FROM korion_payments kp
                  JOIN merchants m ON m.id = kp.merchant_id
             LEFT JOIN partners sp ON sp.id = kp.sales_partner_id
                 WHERE kp.country_master_id = :leaderId
                   AND m.country = :countryScope
                   AND kp.created_at >= :periodStart
                   AND kp.created_at < :periodEnd
                 ORDER BY kp.created_at DESC, kp.id DESC
                 LIMIT 100
                """, params, (rs, rowNum) -> transactionRow(rs, rowNum));

        List<Map<String, Object>> offlineRows = allRows.stream()
                .filter(row -> !"완료".equals(row.get("syncStatus")) || "BLE".equals(row.get("method")) || "NFC".equals(row.get("method")))
                .map(this::offlineTransactionRow)
                .toList();
        List<Map<String, Object>> failedRows = allRows.stream()
                .filter(row -> !"완료".equals(row.get("txStatus")))
                .map(this::failedTransactionRow)
                .toList();

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("stats", List.of(
                stat("todayVolume", "tx.stat.todayVolume", sumAmount(allRows, "amount")),
                stat("todayCount", "tx.stat.todayCount", allRows.size()),
                stat("monthVolume", "tx.stat.monthVolume", sumAmount(allRows, "amount")),
                stat("monthCount", "tx.stat.monthCount", allRows.size()),
                stat("offlineRatio", "tx.stat.offlineRatio", offlineRows.size()),
                stat("failCancel", "tx.stat.failCancel", failedRows.size()),
                stat("syncWait", "tx.stat.syncWait", 0),
                stat("settleWait", "tx.stat.settleWait", 0)
        ));
        result.put("all", Map.of("rows", allRows));
        result.put("offline", Map.of("rows", offlineRows));
        result.put("failed", Map.of("rows", failedRows));
        return result;
    }

    @Override
    public Map<String, Object> findSettlementRequestSummary(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
        MapSqlParameterSource params = baseParams(leaderId, countryScope, periodStart, periodEnd);
        Map<String, Object> amounts = jdbcTemplate.queryForMap("""
                SELECT COALESCE(SUM(kp.amount), 0) AS target_amount,
                       COALESCE(SUM(kp.total_fee), 0) AS total_fee,
                       COALESCE(SUM(kp.country_master_fee), 0) AS leader_fee,
                       COALESCE(SUM(kp.sales_partner_fee), 0) AS partner_fee
                  FROM korion_payments kp
                 WHERE kp.country_master_id = :leaderId
                   AND kp.status = 'COMPLETED'
                   AND kp.paid_at >= :periodStart
                   AND kp.paid_at < :periodEnd
                """, params);
        BigDecimal target = asBigDecimal(amounts.get("target_amount"));
        BigDecimal totalFee = asBigDecimal(amounts.get("total_fee"));
        BigDecimal leaderFee = asBigDecimal(amounts.get("leader_fee"));
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
                stat("targetAmount", "settle.req.stat.targetAmount", kori(target)),
                stat("totalFee", "settle.req.stat.totalFee", kori(totalFee)),
                stat("partnerFee", "settle.req.stat.partnerFee", kori(partnerFee)),
                stat("directFee", "settle.req.stat.directFee", kori(leaderFee)),
                stat("autoSettle", "settle.req.stat.autoSettle", kori(partnerFee)),
                stat("held", "settle.req.stat.held", "0 KORI"),
                stat("finalAmount", "settle.req.stat.finalAmount", kori(leaderFee)),
                stat("problemCount", "settle.req.stat.problemCount", "0건")
        ));
        result.put("calc", Map.of(
                "partnerProfit", amountText(partnerFee),
                "directProfit", amountText(leaderFee),
                "held", "0",
                "final", amountText(leaderFee),
                "unit", "KORI",
                "gauges", List.of()
        ));
        result.put("feeStructure", List.of());
        result.put("autoDesc", "본사 승인 후 하위 파트너 수수료까지 자동 정산 대상에 포함됩니다.");
        result.put("autoHighlightTitle", "하부 파트너 자동 정산 예정 " + kori(partnerFee));
        result.put("autoHighlightDesc", "리더 정산 요청 승인 후 파트너 지급 대상으로 분리됩니다.");
        result.put("autoStats", List.of(
                stat("partnerFee", "settle.req.stat.partnerFee", kori(partnerFee)),
                stat("directFee", "settle.req.auto.directFeeProfit", kori(leaderFee)),
                stat("autoSettle", "settle.req.stat.autoSettle", kori(partnerFee)),
                stat("held", "settle.req.stat.held", "0 KORI")
        ));
        result.put("partnerTable", Map.of("descKey", "settle.req.pt.desc", "rows", List.of()));
        result.put("directTable", Map.of("descKey", "settle.req.dt.desc", "rows", List.of()));
        result.put("heldTable", Map.of("descKey", "settle.req.ht.desc", "rows", List.of()));
        result.put("summary", Map.of(
                "period", DATE.format(periodStart) + " ~ " + DATE.format(periodEnd.minusDays(1)),
                "lastDate", DATE.format(periodStart.minusDays(1)),
                "finalAmount", kori(leaderFee),
                "autoSettle", kori(partnerFee),
                "held", "0 KORI",
                "requestAmount", kori(leaderFee),
                "wallet", "-"
        ));
        result.put("checks", List.of("권한 범위 확인", "정산 가능 기간 확인", "보류 거래 제외"));
        result.put("form", Map.of("fields", List.of()));
        return result;
    }

    @Override
    public Map<String, Object> findSettlementHistory(long leaderId, String countryScope) {
        MapSqlParameterSource params = baseParams(leaderId, countryScope);
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT sr.id, sr.request_no, sr.period_start, sr.period_end,
                       sr.requested_amount, sr.approved_amount, sr.held_amount, sr.status,
                       sr.requested_at, sr.paid_at,
                       COALESCE(SUM(ce.gross_amount), 0) AS total_amount,
                       COALESCE(SUM(CASE WHEN ce.beneficiary_type = 'PARTNER' THEN ce.commission_amount ELSE 0 END), 0) AS partner_amount
                  FROM distributor_settlement_requests sr
             LEFT JOIN distributor_commission_entries ce ON ce.settlement_request_id = sr.id
                 WHERE sr.recipient_type = 'LEADER'
                   AND sr.recipient_partner_id = :leaderId
                 GROUP BY sr.id, sr.request_no, sr.period_start, sr.period_end,
                          sr.requested_amount, sr.approved_amount, sr.held_amount, sr.status,
                          sr.requested_at, sr.paid_at
                 ORDER BY sr.requested_at DESC, sr.id DESC
                 LIMIT 50
                """, params, (rs, rowNum) -> settlementHistoryRow(rs, true));
        String lastSettleDate = lastPaidSettlementDate(params, "LEADER");
        BigDecimal thisRequestAmount = pendingSettlementAmount(params, "LEADER");
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("lastSettleDate", lastSettleDate);
        result.put("thisRequestAmount", kori(thisRequestAmount));
        result.put("rows", rows);
        return result;
    }

    @Override
    public Map<String, Object> findSettlementDetail(long leaderId, String countryScope) {
        MapSqlParameterSource params = baseParams(leaderId, countryScope);
        List<Map<String, Object>> details = jdbcTemplate.query("""
                SELECT sr.id, sr.request_no, sr.period_start, sr.period_end,
                       sr.requested_amount, sr.approved_amount, sr.held_amount, sr.status,
                       sr.requested_at, sr.paid_at,
                       COALESCE(SUM(ce.gross_amount), 0) AS total_amount,
                       COALESCE(SUM(ce.fee_amount), 0) AS total_fee,
                       COALESCE(SUM(CASE WHEN ce.beneficiary_type = 'PARTNER' THEN ce.commission_amount ELSE 0 END), 0) AS partner_amount,
                       COALESCE(SUM(CASE WHEN ce.beneficiary_type = 'LEADER' THEN ce.commission_amount ELSE 0 END), 0) AS leader_amount
                  FROM distributor_settlement_requests sr
             LEFT JOIN distributor_commission_entries ce ON ce.settlement_request_id = sr.id
                 WHERE sr.recipient_type = 'LEADER'
                   AND sr.recipient_partner_id = :leaderId
                 GROUP BY sr.id, sr.request_no, sr.period_start, sr.period_end,
                          sr.requested_amount, sr.approved_amount, sr.held_amount, sr.status,
                          sr.requested_at, sr.paid_at
                 ORDER BY sr.requested_at DESC, sr.id DESC
                 LIMIT 1
                """, params, (rs, rowNum) -> leaderSettlementDetail(rs, leaderId));
        if (!details.isEmpty()) {
            Map<String, Object> detail = details.get(0);
            long settlementRequestId = ((Number) detail.get("settlementRequestId")).longValue();
            detail.remove("settlementRequestId");
            detail.put("partnerRows", settlementPartnerRows(settlementRequestId));
            detail.put("merchantRows", settlementMerchantRows(settlementRequestId, "LEADER"));
            detail.put("heldRows", settlementHeldRows(settlementRequestId));
            return detail;
        }
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("no", "-");
        result.put("status", "내역 없음");
        result.put("basicInfo", List.of());
        result.put("amountSummary", List.of());
        result.put("partnerRows", List.of());
        result.put("merchantRows", List.of());
        result.put("heldRows", List.of());
        return result;
    }

    @Override
    public Map<String, Object> findHqNotices(long leaderId, String countryScope) {
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, title, created_at, is_important
                  FROM notices
                 ORDER BY created_at DESC, id DESC
                 LIMIT 50
                """, Map.of(), (rs, rowNum) -> {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("no", "N-" + rs.getLong("id"));
            row.put("author", "본사");
            row.put("target", "리더/파트너");
            row.put("title", rs.getString("title"));
            row.put("sentDate", date(rs.getTimestamp("created_at")));
            row.put("read", rs.getBoolean("is_important") ? "확인필요" : "읽음");
            return row;
        });
        return Map.of("rows", rows);
    }

    @Override
    public Map<String, Object> findNoticeSendSummary(long leaderId, String countryScope) {
        long partnerCount = countBy("""
                SELECT COUNT(*) FROM partners
                 WHERE parent_partner_id = :leaderId
                   AND COALESCE(assigned_country, country) = :countryScope
                """, new MapSqlParameterSource().addValue("leaderId", leaderId).addValue("countryScope", countryScope));
        long merchantCount = countBy("""
                SELECT COUNT(*) FROM merchants
                 WHERE parent_country_master_id = :leaderId
                   AND country = :countryScope
                """, new MapSqlParameterSource().addValue("leaderId", leaderId).addValue("countryScope", countryScope));
        return Map.of("metrics", List.of(
                metric("partner", "notice.send.kpi.partner", partnerCount + "명"),
                metric("merchant", "notice.send.kpi.merchant", merchantCount + "개"),
                metric("today", "notice.send.kpi.today", "0건"),
                metric("scheduled", "notice.send.kpi.scheduled", "0건")
        ));
    }

    @Override
    public Map<String, Object> findNoticeHistory(long leaderId, String countryScope) {
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
    public Map<String, Object> findProfile(long leaderId, String countryScope) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("leaderId", leaderId)
                .addValue("countryScope", countryScope);
        return jdbcTemplate.queryForObject("""
                SELECT p.id, p.status, COALESCE(p.assigned_country, p.country) AS country,
                       p.region, p.city, p.created_at, p.approved_at,
                       u.login_id, u.name, u.nickname, u.phone, u.country_code,
                       COUNT(DISTINCT m.id) AS merchant_count,
                       COALESCE(SUM(pcl.amount), 0) AS unpaid_fee
                  FROM partners p
                  JOIN users u ON u.id = p.user_id
             LEFT JOIN merchants m ON m.parent_country_master_id = p.id
             LEFT JOIN partner_commission_ledger pcl ON pcl.partner_id = p.id AND pcl.status = 'CONFIRMED'
                 WHERE p.id = :leaderId
                   AND COALESCE(p.assigned_country, p.country) = :countryScope
                 GROUP BY p.id, p.status, COALESCE(p.assigned_country, p.country), p.region, p.city, p.created_at, p.approved_at,
                          u.login_id, u.name, u.nickname, u.phone, u.country_code
                """, params, (rs, rowNum) -> {
            String code = partnerCode(rs.getLong("id"));
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
            result.put("statusItems", List.of(
                    status("profile.status.leaderCode", code),
                    status("profile.status.country", rs.getString("country")),
                    status("profile.status.city", valueOrDash(rs.getString("city"), rs.getString("region"))),
                    status("profile.status.approval", leaderStatus(rs.getString("status"))),
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
    public Map<String, Object> findActivityLogs(long leaderId, String countryScope) {
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

    private MapSqlParameterSource baseParams(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
        return new MapSqlParameterSource()
                .addValue("leaderId", leaderId)
                .addValue("countryScope", countryScope)
                .addValue("periodStart", periodStart)
                .addValue("periodEnd", periodEnd);
    }

    private MapSqlParameterSource baseParams(long leaderId, String countryScope) {
        return new MapSqlParameterSource()
                .addValue("leaderId", leaderId)
                .addValue("countryScope", countryScope);
    }

    private MapSqlParameterSource partnerParams(long leaderId, String countryScope, PartnerSearchCriteria criteria) {
        return new MapSqlParameterSource()
                .addValue("leaderId", leaderId)
                .addValue("countryScope", countryScope)
                .addValue("keyword", criteria.keyword() == null || criteria.keyword().isBlank()
                        ? null
                        : "%" + criteria.keyword().toLowerCase() + "%")
                .addValue("status", criteria.status() == null || criteria.status().isBlank() ? null : criteria.status())
                .addValue("region", criteria.region() == null || criteria.region().isBlank() ? null : criteria.region());
    }

    private List<Map<String, Object>> merchantSalesRows(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
        MapSqlParameterSource params = baseParams(leaderId, countryScope, periodStart, periodEnd);
        return jdbcTemplate.query("""
                SELECT m.id, m.merchant_code, m.merchant_name, m.city,
                       COALESCE(sp.id, 0) AS partner_id,
                       COALESCE(SUM(kp.amount), 0) AS month_revenue,
                       COUNT(kp.id) AS month_count,
                       COALESCE(SUM(kp.total_fee), 0) AS fee,
                       MAX(kp.paid_at) AS recent_pay
                  FROM merchants m
             LEFT JOIN partners sp ON sp.id = m.parent_sales_partner_id
             LEFT JOIN korion_payments kp ON kp.merchant_id = m.id
                   AND kp.paid_at >= :periodStart
                   AND kp.paid_at < :periodEnd
                   AND kp.status = 'COMPLETED'
                 WHERE m.parent_country_master_id = :leaderId
                   AND m.country = :countryScope
                 GROUP BY m.id, m.merchant_code, m.merchant_name, m.city, sp.id
                 ORDER BY month_revenue DESC, m.id DESC
                 LIMIT 50
                """, params, (rs, rowNum) -> {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("no", String.valueOf(rowNum + 1));
            row.put("partner", rs.getLong("partner_id") > 0 ? partnerCode(rs.getLong("partner_id")) : "직접 계약");
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

    private LinkedHashMap<String, Object> salesPage(List<Map<String, Object>> t1Rows, List<Map<String, Object>> merchantRows, LocalDate periodStart, LocalDate periodEnd) {
        LinkedHashMap<String, Object> result = salesSummary(periodStart, periodEnd);
        result.put("t1Rows", t1Rows);
        result.put("merchantRows", merchantRows);
        return result;
    }

    private LinkedHashMap<String, Object> salesSummary(LocalDate periodStart, LocalDate periodEnd) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("stats", List.of(
                stat("todayVolume", "partnerSales.stat.todayVolume", "0 KORI"),
                stat("todayCount", "partnerSales.stat.todayCount", "0"),
                stat("monthRevenue", "partnerSales.stat.monthRevenue", "0 KORI"),
                stat("monthCount", "partnerSales.stat.monthCount", "0"),
                stat("monthFee", "partnerSales.stat.monthFee", "0 KORI"),
                stat("unsettledFee", "partnerSales.stat.unsettledFee", "0 KORI"),
                stat("offlineRatio", "partnerSales.stat.offlineRatio", "0/0/0"),
                stat("failCancel", "partnerSales.stat.failCancel", "0")
        ));
        return result;
    }

    private Map<String, Object> transactionRow(ResultSet rs, int rowNum) throws SQLException {
        String status = paymentStatus(rs.getString("status"));
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("txNo", String.valueOf(rs.getLong("id")));
        row.put("partner", rs.getLong("partner_id") > 0 ? partnerCode(rs.getLong("partner_id")) : "직접 계약");
        row.put("datetime", dateTime(rs.getTimestamp("paid_at")));
        row.put("merchantCode", valueOrDash(rs.getString("merchant_code")));
        row.put("merchantName", rs.getString("merchant_name"));
        row.put("amount", kori(rs, "amount"));
        row.put("method", "QR");
        row.put("fee", kori(rs, "total_fee"));
        row.put("netAmount", kori(rs, "net_amount_to_merchant"));
        row.put("txStatus", status);
        row.put("syncStatus", "완료");
        return row;
    }

    private Map<String, Object> offlineTransactionRow(Map<String, Object> source) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("txNo", source.get("txNo"));
        row.put("datetime", source.get("datetime"));
        row.put("merchantName", source.get("merchantName"));
        row.put("method", source.get("method"));
        row.put("offlineProof", "생성완료");
        row.put("syncStatus", source.get("syncStatus"));
        row.put("retry", "0");
        row.put("errorCode", "-");
        row.put("amount", source.get("amount"));
        row.put("manualReview", "아니요");
        return row;
    }

    private Map<String, Object> failedTransactionRow(Map<String, Object> source) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("txNo", source.get("txNo"));
        row.put("datetime", source.get("datetime"));
        row.put("partner", source.get("partner"));
        row.put("merchantCode", source.get("merchantCode"));
        row.put("merchantName", source.get("merchantName"));
        row.put("amount", source.get("amount"));
        row.put("failType", source.get("txStatus"));
        row.put("errorCode", "-");
        row.put("reason", "결제 상태 " + source.get("txStatus"));
        row.put("status", source.get("txStatus"));
        return row;
    }

    private Map<String, Object> settlementHistoryRow(ResultSet rs, boolean leader) throws SQLException {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("no", rs.getString("request_no"));
        row.put("appliedDate", date(rs.getTimestamp("requested_at")));
        row.put("period", DATE.format(rs.getDate("period_start").toLocalDate()) + "~ " + DATE.format(rs.getDate("period_end").toLocalDate()));
        row.put("totalAmount", kori(rs, "total_amount"));
        if (leader) {
            row.put("leaderAmount", kori(rs, "requested_amount"));
            row.put("partnerAmount", kori(rs, "partner_amount"));
        } else {
            row.put("partnerAmount", kori(rs, "requested_amount"));
        }
        row.put("held", kori(rs, "held_amount"));
        row.put("status", settlementStatus(rs.getString("status")));
        row.put("paidDate", date(rs.getTimestamp("paid_at")));
        return row;
    }

    private Map<String, Object> leaderSettlementDetail(ResultSet rs, long leaderId) throws SQLException {
        String requestNo = rs.getString("request_no");
        String status = settlementStatus(rs.getString("status"));
        String period = DATE.format(rs.getDate("period_start").toLocalDate()) + " ~ " + DATE.format(rs.getDate("period_end").toLocalDate());
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("settlementRequestId", rs.getLong("id"));
        result.put("no", requestNo);
        result.put("status", status);
        result.put("basicInfo", List.of(
                field("settle.detail.a.no", requestNo),
                field("settle.detail.a.leaderName", partnerCode(leaderId) + " / " + rs.getString("status")),
                field("settle.detail.a.leaderCode", partnerCode(leaderId)),
                field("settle.detail.a.period", period),
                field("settle.detail.a.lastDate", DATE.format(rs.getDate("period_end").toLocalDate())),
                highlightedField("settle.detail.a.appliedDate", date(rs.getTimestamp("requested_at"))),
                field("settle.detail.a.paidDate", date(rs.getTimestamp("paid_at"))),
                field("settle.detail.a.status", status)
        ));
        result.put("amountSummary", List.of(
                field("settle.detail.b.total", kori(rs, "total_amount")),
                field("settle.detail.b.totalFee", kori(rs, "total_fee")),
                field("settle.detail.b.partnerProfit", kori(rs, "partner_amount")),
                field("settle.detail.b.directProfit", kori(rs, "leader_amount")),
                field("settle.detail.b.held", kori(rs, "held_amount")),
                highlightedField("settle.detail.b.finalAmount", kori(rs, "requested_amount")),
                field("settle.detail.b.autoSettle", kori(rs, "partner_amount"))
        ));
        return result;
    }

    private List<Map<String, Object>> settlementPartnerRows(long settlementRequestId) {
        return jdbcTemplate.query("""
                SELECT p.id, p.city,
                       COALESCE(SUM(ce.gross_amount), 0) AS gross_amount,
                       COALESCE(SUM(ce.commission_amount), 0) AS fee,
                       MAX(sr.paid_at) AS paid_at,
                       MAX(ce.settlement_status) AS status
                  FROM distributor_commission_entries ce
                  JOIN partners p ON p.id = ce.beneficiary_partner_id
                  JOIN distributor_settlement_requests sr ON sr.id = ce.settlement_request_id
                 WHERE ce.settlement_request_id = :settlementRequestId
                   AND ce.beneficiary_type = 'PARTNER'
                 GROUP BY p.id, p.city
                 ORDER BY fee DESC, p.id DESC
                 LIMIT 50
                """, Map.of("settlementRequestId", settlementRequestId), (rs, rowNum) -> {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("name", valueOrDash(rs.getString("city"), partnerCode(rs.getLong("id"))));
            row.put("code", partnerCode(rs.getLong("id")));
            row.put("amount", amountText(amount(rs, "gross_amount")));
            row.put("fee", amountText(amount(rs, "fee")));
            row.put("status", settlementStatus(rs.getString("status")));
            row.put("paidDate", date(rs.getTimestamp("paid_at")));
            return row;
        });
    }

    private List<Map<String, Object>> settlementMerchantRows(long settlementRequestId, String beneficiaryType) {
        return jdbcTemplate.query("""
                SELECT m.id, m.merchant_code, m.merchant_name,
                       COALESCE(SUM(ce.gross_amount), 0) AS gross_amount,
                       COALESCE(SUM(ce.commission_amount), 0) AS fee,
                       MAX(ce.settlement_status) AS status
                  FROM distributor_commission_entries ce
                  JOIN merchants m ON m.id = ce.merchant_id
                 WHERE ce.settlement_request_id = :settlementRequestId
                   AND ce.beneficiary_type = :beneficiaryType
                 GROUP BY m.id, m.merchant_code, m.merchant_name
                 ORDER BY fee DESC, m.id DESC
                 LIMIT 50
                """, new MapSqlParameterSource()
                .addValue("settlementRequestId", settlementRequestId)
                .addValue("beneficiaryType", beneficiaryType), (rs, rowNum) -> {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("name", rs.getString("merchant_name"));
            row.put("code", valueOrDash(rs.getString("merchant_code"), merchantCode(rs.getLong("id"))));
            row.put("amount", amountText(amount(rs, "gross_amount")));
            row.put("fee", amountText(amount(rs, "fee")));
            row.put("status", settlementStatus(rs.getString("status")));
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
            row.put("partner", rs.wasNull() ? "직접 계약" : partnerCode(partnerId));
            row.put("reason", valueOrDash(rs.getString("hold_reason"), settlementStatus(rs.getString("settlement_status"))));
            row.put("amount", amountText(amount(rs, "gross_amount")));
            row.put("heldFee", amountText(amount(rs, "commission_amount")));
            row.put("status", settlementStatus(rs.getString("settlement_status")));
            return row;
        });
    }

    private String lastPaidSettlementDate(MapSqlParameterSource params, String recipientType) {
        List<String> rows = jdbcTemplate.query("""
                SELECT paid_at
                  FROM distributor_settlement_requests
                 WHERE recipient_type = :recipientType
                   AND recipient_partner_id = :leaderId
                   AND status = 'PAID'
                   AND paid_at IS NOT NULL
                 ORDER BY paid_at DESC
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValues(params.getValues())
                .addValue("recipientType", recipientType), (rs, rowNum) -> date(rs.getTimestamp("paid_at")));
        return rows.stream().findFirst().orElse("-");
    }

    private BigDecimal pendingSettlementAmount(MapSqlParameterSource params, String recipientType) {
        BigDecimal amount = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(requested_amount), 0)
                  FROM distributor_settlement_requests
                 WHERE recipient_type = :recipientType
                   AND recipient_partner_id = :leaderId
                   AND status IN ('DRAFT', 'REQUESTED', 'REVIEWING', 'APPROVED', 'HELD', 'ADJUSTMENT_REQUIRED')
                """, new MapSqlParameterSource()
                .addValues(params.getValues())
                .addValue("recipientType", recipientType), BigDecimal.class);
        return amount == null ? ZERO : amount;
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

    private static LinkedHashMap<String, Object> stat(String id, String labelKey, long value) {
        return stat(id, labelKey, String.valueOf(value));
    }

    private static LinkedHashMap<String, Object> stat(String id, String labelKey, Object value) {
        LinkedHashMap<String, Object> stat = new LinkedHashMap<>();
        stat.put("id", id);
        stat.put("labelKey", labelKey);
        stat.put("value", value);
        return stat;
    }

    private static LinkedHashMap<String, Object> metric(String id, String labelKey, String value) {
        LinkedHashMap<String, Object> metric = new LinkedHashMap<>();
        metric.put("id", id);
        metric.put("labelKey", labelKey);
        metric.put("value", value);
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

    private static String applicationCode(String applicantType, long id) {
        return ("PARTNER".equals(applicantType) ? "APP-SP-" : "APP-MER-") + String.format("%05d", id);
    }

    private static String partnerCode(long id) {
        return "SP-" + String.format("%05d", id);
    }

    private static String merchantCode(long id) {
        return "MER-" + String.format("%05d", id);
    }

    private static String signupStatus(String status) {
        return switch (status) {
            case "APPROVED" -> "승인";
            case "REVIEWING" -> "검토중";
            case "NEED_MORE_INFO" -> "자료요청";
            case "HOLD" -> "보류";
            case "REJECTED" -> "거절";
            default -> "승인요청";
        };
    }

    private static String signupOperationStatus(String status) {
        return "APPROVED".equals(status) ? "활성" : "대기";
    }

    private static String leaderStatus(String status) {
        return "COUNTRY_LEADER_APPROVED".equals(status) ? "승인" : valueOrDash(status);
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

    private static String sumAmount(List<Map<String, Object>> rows, String key) {
        return rows.stream()
                .map(row -> String.valueOf(row.getOrDefault(key, "0 KORI")).replace(" KORI", ""))
                .map(value -> {
                    try {
                        return new BigDecimal(value);
                    } catch (NumberFormatException ignored) {
                        return ZERO;
                    }
                })
                .reduce(ZERO, BigDecimal::add)
                .stripTrailingZeros()
                .toPlainString() + " KORI";
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

    private RowMapper<LeaderPartnerResponse.PartnerSummary> partnerMapper() {
        return (rs, rowNum) -> new LeaderPartnerResponse.PartnerSummary(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("login_id"),
                rs.getString("country"),
                rs.getString("region"),
                rs.getString("city"),
                rs.getString("status"),
                rs.getLong("merchant_count"),
                amount(rs, "completed_transaction_amount"),
                instant(rs, "last_activity_at")
        );
    }

    private static BigDecimal amount(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? ZERO : value;
    }

    private static java.time.Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
