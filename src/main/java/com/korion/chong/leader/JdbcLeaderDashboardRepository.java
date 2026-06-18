package com.korion.chong.leader;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
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

    private MapSqlParameterSource baseParams(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd) {
        return new MapSqlParameterSource()
                .addValue("leaderId", leaderId)
                .addValue("countryScope", countryScope)
                .addValue("periodStart", periodStart)
                .addValue("periodEnd", periodEnd);
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
