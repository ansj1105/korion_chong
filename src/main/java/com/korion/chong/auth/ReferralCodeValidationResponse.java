package com.korion.chong.auth;

public record ReferralCodeValidationResponse(
        boolean valid,
        String code,
        String codeType,
        Long ownerPartnerId,
        String country,
        String city,
        String resultCode,
        String messageKey
) {
    public static ReferralCodeValidationResponse invalid(String code) {
        return new ReferralCodeValidationResponse(false, code, null, null, null, null, "INVALID_CODE", "auth.referral.invalid");
    }

    public static ReferralCodeValidationResponse invalidFormat(String code) {
        return new ReferralCodeValidationResponse(false, code, null, null, null, null, "INVALID_CODE_FORMAT", "auth.referral.invalidFormat");
    }
}
