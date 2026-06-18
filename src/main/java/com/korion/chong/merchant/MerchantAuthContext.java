package com.korion.chong.merchant;

import java.util.Set;

public record MerchantAuthContext(
        long merchantId,
        Set<String> countryScopes
) {
    public boolean canAccess(String countryScope) {
        return countryScopes.contains(countryScope);
    }
}
