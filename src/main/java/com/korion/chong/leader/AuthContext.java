package com.korion.chong.leader;

import java.util.Set;

public record AuthContext(
        long leaderId,
        Set<String> countryScopes
) {
    public boolean canAccess(String countryScope) {
        return countryScopes.contains(countryScope);
    }
}
