package com.korion.chong.merchant;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MerchantAuthContextFactory {
    public MerchantAuthContext fromHeaders(String merchantIdHeader, String countryScopesHeader) {
        long merchantId;
        try {
            merchantId = Long.parseLong(merchantIdHeader);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("X-Merchant-Id must be a numeric merchant id");
        }
        Set<String> countryScopes = Arrays.stream(countryScopesHeader.split(","))
                .map(String::trim)
                .filter(scope -> !scope.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        if (countryScopes.isEmpty()) {
            throw new IllegalArgumentException("X-Country-Scopes must contain at least one country scope");
        }
        return new MerchantAuthContext(merchantId, countryScopes);
    }
}
