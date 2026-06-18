package com.korion.chong.leader;

public class ForbiddenCountryScopeException extends RuntimeException {
    public ForbiddenCountryScopeException(String countryScope) {
        super("Leader cannot access country scope: " + countryScope);
    }
}
