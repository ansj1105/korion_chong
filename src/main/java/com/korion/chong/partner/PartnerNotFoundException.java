package com.korion.chong.partner;

public class PartnerNotFoundException extends RuntimeException {
    public PartnerNotFoundException(long partnerId) {
        super("Partner was not found or is not an approved sales partner: " + partnerId);
    }
}
