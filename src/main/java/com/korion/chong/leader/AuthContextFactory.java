package com.korion.chong.leader;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AuthContextFactory {
    public AuthContext fromHeaders(String leaderIdHeader, String countryScopesHeader) {
        long leaderId;
        try {
            leaderId = Long.parseLong(leaderIdHeader);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("X-Leader-Id must be a numeric leader partner id");
        }

        Set<String> countryScopes = Arrays.stream(countryScopesHeader.split(","))
                .map(String::trim)
                .filter(scope -> !scope.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        if (countryScopes.isEmpty()) {
            throw new IllegalArgumentException("X-Country-Scopes must contain at least one country scope");
        }
        return new AuthContext(leaderId, countryScopes);
    }
}
