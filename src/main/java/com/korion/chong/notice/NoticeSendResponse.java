package com.korion.chong.notice;

import java.time.Instant;

public record NoticeSendResponse(
        long noticeId,
        String status,
        Instant scheduledAt,
        Instant sentAt,
        int recipientCount,
        String resultCode,
        String messageKey
) {
}
