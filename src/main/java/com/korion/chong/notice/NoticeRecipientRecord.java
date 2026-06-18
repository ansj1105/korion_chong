package com.korion.chong.notice;

record NoticeRecipientRecord(
        String recipientType,
        Long recipientUserId,
        Long recipientPartnerId,
        Long recipientMerchantId
) {
}
