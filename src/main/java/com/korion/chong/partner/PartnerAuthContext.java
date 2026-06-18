package com.korion.chong.partner;

import java.util.Set;

public record PartnerAuthContext(
        long partnerId,
        Set<String> countryScopes
) {
    public boolean canAccess(String countryScope) {
        return countryScopes.contains(countryScope);
    }
}
