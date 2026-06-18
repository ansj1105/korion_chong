package com.korion.chong.leader;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaderDashboardRepository {
    Optional<LeaderProfile> findLeaderProfile(long leaderId);

    LeaderDashboardResponse.Kpis findKpis(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd);

    List<LeaderDashboardResponse.MonthlyVolume> findMonthlyVolume(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd);

    LeaderDashboardResponse.FeeSummary findFeeSummary(long leaderId, String countryScope, LocalDate periodStart, LocalDate periodEnd);

    List<LeaderPartnerResponse.PartnerSummary> findPartners(
            long leaderId,
            String countryScope,
            PartnerSearchCriteria criteria
    );

    long countPartners(long leaderId, String countryScope, PartnerSearchCriteria criteria);
}
