package com.korion.chong.merchant;

public record MerchantProfile(
        long merchantId,
        long userId,
        String loginId,
        String merchantName,
        String status,
        String countryScope,
        String region,
        String city
) {
}
