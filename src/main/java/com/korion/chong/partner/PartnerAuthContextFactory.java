package com.korion.chong.partner;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PartnerAuthContextFactory {
    public PartnerAuthContext fromHeaders(String partnerIdHeader, String countryScopesHeader) {
        long partnerId;
        try {
            partnerId = Long.parseLong(partnerIdHeader);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("X-Partner-Id must be a numeric partner id");
        }

        Set<String> countryScopes = Arrays.stream(countryScopesHeader.split(","))
                .map(String::trim)
                .filter(scope -> !scope.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        if (countryScopes.isEmpty()) {
            throw new IllegalArgumentException("X-Country-Scopes must contain at least one country scope");
        }
        return new PartnerAuthContext(partnerId, countryScopes);
    }
}
