package com.korion.chong.leader;

import java.util.List;

public record LeaderProfile(
        long leaderId,
        long userId,
        String loginId,
        String status,
        List<String> countryScopes
) {
}
