package com.korion.chong.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.sql.ResultSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class JdbcAuthRepositoryTest {
    private final NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
    private final JdbcAuthRepository repository = new JdbcAuthRepository(jdbcTemplate);

    @Test
    void merchantSignupStoresOwnerSalesPartnerFromReferralCode() throws Exception {
        Mockito.when(jdbcTemplate.query(Mockito.contains("FROM referral_codes"), any(java.util.Map.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<ReferralCodeValidationResponse> mapper = invocation.getArgument(2);
                    ResultSet rs = Mockito.mock(ResultSet.class);
                    Mockito.when(rs.getString("code")).thenReturn("SP20");
                    Mockito.when(rs.getString("code_type")).thenReturn("SALES_PARTNER");
                    Mockito.when(rs.getObject("owner_partner_id", Long.class)).thenReturn(20L);
                    Mockito.when(rs.getString("country")).thenReturn("KR");
                    Mockito.when(rs.getString("city")).thenReturn("Seoul");
                    return List.of(mapper.mapRow(rs, 0));
                });
        Mockito.when(jdbcTemplate.queryForObject(Mockito.contains("SELECT id FROM referral_codes"), any(java.util.Map.class), eq(Long.class)))
                .thenReturn(700L);
        Mockito.when(jdbcTemplate.query(Mockito.contains("FROM partners"), any(java.util.Map.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<Long> mapper = invocation.getArgument(2);
                    ResultSet rs = Mockito.mock(ResultSet.class);
                    Mockito.when(rs.getLong("id")).thenReturn(20L);
                    return List.of(mapper.mapRow(rs, 0));
                });
        Mockito.when(jdbcTemplate.queryForObject(Mockito.contains("INSERT INTO distributor_signup_applications"), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(501L);

        long applicationId = repository.createSignupApplication(signupRequest(), "hash");

        ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        Mockito.verify(jdbcTemplate).queryForObject(Mockito.contains("INSERT INTO distributor_signup_applications"), params.capture(), eq(Long.class));
        assertThat(applicationId).isEqualTo(501L);
        assertThat(params.getValue().getValue("referralCodeId")).isEqualTo(700L);
        assertThat(params.getValue().getValue("ownerPartnerId")).isEqualTo(20L);
    }

    private SignupApplicationRequest signupRequest() {
        return new SignupApplicationRequest(
                "MERCHANT",
                "merchant.kr",
                "password123",
                "merchant@example.com",
                "Merchant Co",
                "Kim",
                "010",
                "@merchant",
                null,
                "SP20",
                "KR",
                "Seoul",
                "Seoul",
                "address",
                "Cafe",
                "TXYZ",
                null,
                null,
                "req-signup"
        );
    }
}
