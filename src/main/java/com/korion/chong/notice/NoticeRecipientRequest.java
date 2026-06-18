package com.korion.chong.notice;

import jakarta.validation.constraints.NotBlank;

public record NoticeRecipientRequest(
        @NotBlank String recipientType,
        Long recipientPartnerId,
        Long recipientMerchantId
) {
}
