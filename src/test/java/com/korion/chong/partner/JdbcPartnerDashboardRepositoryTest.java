package com.korion.chong.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class JdbcPartnerDashboardRepositoryTest {
    private final NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
    private final JdbcPartnerDashboardRepository repository = new JdbcPartnerDashboardRepository(jdbcTemplate);

    @Test
    void merchantApplicationsReturnRowsFromOwnedSignupApplications() throws Exception {
        Mockito.when(jdbcTemplate.queryForObject(any(String.class), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(2L, 1L, 1L, 0L);
        Mockito.when(jdbcTemplate.query(any(String.class), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<Map<String, Object>> mapper = invocation.getArgument(2);
                    ResultSet rs = Mockito.mock(ResultSet.class);
                    Mockito.when(rs.getLong("id")).thenReturn(42L);
                    Mockito.when(rs.getString("company_name")).thenReturn("Seoul Mart");
                    Mockito.when(rs.getString("contact_name")).thenReturn("Lee");
                    Mockito.when(rs.getString("telegram")).thenReturn("@seoulmart");
                    Mockito.when(rs.getString("region")).thenReturn("Seoul");
                    Mockito.when(rs.getString("city")).thenReturn("Gangnam");
                    Mockito.when(rs.getString("business_type")).thenReturn("Cafe");
                    Mockito.when(rs.getString("status")).thenReturn("REQUESTED");
                    Mockito.when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.parse("2026-06-19T00:00:00Z")));
                    return List.of(mapper.mapRow(rs, 0));
                });

        Map<String, Object> response = repository.findMerchantApplications(20L, "KR");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) response.get("rows");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("applicationId", 42L);
        assertThat(rows.get(0)).containsEntry("code", "REQ-00042");
        assertThat(rows.get(0)).containsEntry("name", "Seoul Mart");
        assertThat(rows.get(0)).containsEntry("opStatus", "승인신청");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jdbcTemplate).query(sql.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        assertThat(sql.getValue()).contains("FROM distributor_signup_applications");
        assertThat(sql.getValue()).contains("owner_partner_id = :partnerId");
    }

    @Test
    void settlementHistoryReturnsRowsFromSettlementRequests() throws Exception {
        Mockito.when(jdbcTemplate.query(Mockito.contains("FROM distributor_settlement_requests sr"), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<Map<String, Object>> mapper = invocation.getArgument(2);
                    ResultSet rs = settlementResultSet();
                    return List.of(mapper.mapRow(rs, 0));
                });
        Mockito.when(jdbcTemplate.query(Mockito.contains("SELECT paid_at"), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of("2026.06.19"));
        Mockito.when(jdbcTemplate.queryForObject(Mockito.contains("SUM(requested_amount)"), any(MapSqlParameterSource.class), eq(BigDecimal.class)))
                .thenReturn(new BigDecimal("100.00"));

        Map<String, Object> response = repository.findSettlementHistory(20L, "KR");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) response.get("rows");
        assertThat(response).containsEntry("lastSettleDate", "2026.06.19");
        assertThat(response).containsEntry("thisRequestAmount", "100 KORI");
        assertThat(rows.get(0)).containsEntry("no", "SET-20");
        assertThat(rows.get(0)).containsEntry("partnerAmount", "100 KORI");
        assertThat(rows.get(0)).containsEntry("status", "본사 검토중");
    }

    private ResultSet settlementResultSet() throws Exception {
        ResultSet rs = Mockito.mock(ResultSet.class);
        Mockito.when(rs.getString("request_no")).thenReturn("SET-20");
        Mockito.when(rs.getDate("period_start")).thenReturn(java.sql.Date.valueOf("2026-06-01"));
        Mockito.when(rs.getDate("period_end")).thenReturn(java.sql.Date.valueOf("2026-06-10"));
        Mockito.when(rs.getBigDecimal("requested_amount")).thenReturn(new BigDecimal("100.00"));
        Mockito.when(rs.getBigDecimal("held_amount")).thenReturn(new BigDecimal("5.00"));
        Mockito.when(rs.getBigDecimal("total_amount")).thenReturn(new BigDecimal("1000.00"));
        Mockito.when(rs.getString("status")).thenReturn("REQUESTED");
        Mockito.when(rs.getTimestamp("requested_at")).thenReturn(Timestamp.from(Instant.parse("2026-06-19T00:00:00Z")));
        Mockito.when(rs.getTimestamp("paid_at")).thenReturn(null);
        return rs;
    }
}
