package com.korion.chong.auth;

public class AuthValidationException extends RuntimeException {
    private final String code;

    public AuthValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
