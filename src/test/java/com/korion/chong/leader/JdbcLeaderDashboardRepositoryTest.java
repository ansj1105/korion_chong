package com.korion.chong.leader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class JdbcLeaderDashboardRepositoryTest {
    private final NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
    private final JdbcLeaderDashboardRepository repository = new JdbcLeaderDashboardRepository(jdbcTemplate);

    @Test
    void settlementHistoryReturnsLeaderScopedSettlementRows() throws Exception {
        Mockito.when(jdbcTemplate.query(Mockito.contains("FROM distributor_settlement_requests sr"), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<Map<String, Object>> mapper = invocation.getArgument(2);
                    ResultSet rs = settlementResultSet();
                    return List.of(mapper.mapRow(rs, 0));
                });
        Mockito.when(jdbcTemplate.query(Mockito.contains("SELECT paid_at"), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of("2026.06.19"));
        Mockito.when(jdbcTemplate.queryForObject(Mockito.contains("SUM(requested_amount)"), any(MapSqlParameterSource.class), eq(BigDecimal.class)))
                .thenReturn(new BigDecimal("150.00"));

        Map<String, Object> response = repository.findSettlementHistory(10L, "KR");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) response.get("rows");
        assertThat(response).containsEntry("thisRequestAmount", "150 KORI");
        assertThat(rows.get(0)).containsEntry("leaderAmount", "100 KORI");
        assertThat(rows.get(0)).containsEntry("partnerAmount", "20 KORI");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jdbcTemplate, Mockito.atLeastOnce()).query(sql.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        assertThat(sql.getAllValues()).anySatisfy(value -> {
            assertThat(value).contains("sr.recipient_type = 'LEADER'");
            assertThat(value).contains("sr.recipient_partner_id = :leaderId");
        });
    }

    private ResultSet settlementResultSet() throws Exception {
        ResultSet rs = Mockito.mock(ResultSet.class);
        Mockito.when(rs.getString("request_no")).thenReturn("SET-10");
        Mockito.when(rs.getDate("period_start")).thenReturn(java.sql.Date.valueOf("2026-06-01"));
        Mockito.when(rs.getDate("period_end")).thenReturn(java.sql.Date.valueOf("2026-06-10"));
        Mockito.when(rs.getBigDecimal("requested_amount")).thenReturn(new BigDecimal("100.00"));
        Mockito.when(rs.getBigDecimal("held_amount")).thenReturn(new BigDecimal("5.00"));
        Mockito.when(rs.getBigDecimal("total_amount")).thenReturn(new BigDecimal("1000.00"));
        Mockito.when(rs.getBigDecimal("partner_amount")).thenReturn(new BigDecimal("20.00"));
        Mockito.when(rs.getString("status")).thenReturn("REQUESTED");
        Mockito.when(rs.getTimestamp("requested_at")).thenReturn(Timestamp.from(Instant.parse("2026-06-19T00:00:00Z")));
        Mockito.when(rs.getTimestamp("paid_at")).thenReturn(null);
        return rs;
    }
}
