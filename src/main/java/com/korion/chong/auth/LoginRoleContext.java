package com.korion.chong.auth;

import java.util.List;

public record LoginRoleContext(
        String role,
        Long partnerId,
        Long merchantId,
        List<String> countryScopes
) {
}
