package com.korion.chong.merchant;

public class MerchantNotFoundException extends RuntimeException {
    public MerchantNotFoundException(long merchantId) {
        super("merchant was not found or is not approved: " + merchantId);
    }
}
