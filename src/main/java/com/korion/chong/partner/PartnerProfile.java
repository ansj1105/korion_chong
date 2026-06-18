package com.korion.chong.partner;

public record PartnerProfile(
        long partnerId,
        long userId,
        String loginId,
        String status,
        String countryScope,
        String region,
        String city
) {
}
