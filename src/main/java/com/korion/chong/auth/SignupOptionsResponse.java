package com.korion.chong.auth;

import java.util.List;

public record SignupOptionsResponse(
        List<SignupCountryOption> countries
) {
}
