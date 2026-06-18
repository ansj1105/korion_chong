package com.korion.chong.notice;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NoticeRepository {
    Optional<NoticeRecipientRecord> findLeaderRecipient(long leaderPartnerId, NoticeRecipientRequest recipient);

    Optional<NoticeRecipientRecord> findPartnerRecipient(long partnerId, NoticeRecipientRequest recipient);

    long createNotice(
            String senderType,
            long senderUserId,
            long senderPartnerId,
            NoticeSendRequest request,
            String status,
            Instant now
    );

    void createRecipients(long noticeId, List<NoticeRecipientRecord> recipients, String deliveryStatus, Instant deliveredAt);

    void recordActivity(String role, String actionType, String status, long noticeId, String requestId);
}
