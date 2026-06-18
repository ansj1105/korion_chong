package com.korion.chong.notice;

import com.korion.chong.leader.AuthContext;
import com.korion.chong.leader.ForbiddenCountryScopeException;
import com.korion.chong.leader.LeaderDashboardRepository;
import com.korion.chong.leader.LeaderNotFoundException;
import com.korion.chong.leader.LeaderProfile;
import com.korion.chong.partner.PartnerAuthContext;
import com.korion.chong.partner.PartnerDashboardRepository;
import com.korion.chong.partner.PartnerNotFoundException;
import com.korion.chong.partner.PartnerProfile;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoticeService {
    private final NoticeRepository repository;
    private final LeaderDashboardRepository leaderRepository;
    private final PartnerDashboardRepository partnerRepository;
    private final Clock clock;

    public NoticeService(
            NoticeRepository repository,
            LeaderDashboardRepository leaderRepository,
            PartnerDashboardRepository partnerRepository,
            Clock clock
    ) {
        this.repository = repository;
        this.leaderRepository = leaderRepository;
        this.partnerRepository = partnerRepository;
        this.clock = clock;
    }

    @Transactional
    public NoticeSendResponse sendLeaderNotice(AuthContext authContext, NoticeSendRequest request) {
        LeaderProfile profile = leaderRepository.findLeaderProfile(authContext.leaderId())
                .orElseThrow(() -> new LeaderNotFoundException(authContext.leaderId()));
        List<NoticeRecipientRecord> recipients = request.recipients().stream()
                .map(recipient -> repository.findLeaderRecipient(authContext.leaderId(), recipient)
                        .orElseThrow(() -> new ForbiddenCountryScopeException("noticeRecipient:" + recipient)))
                .toList();
        return createNotice("LEADER", profile.userId(), authContext.leaderId(), "LEADER", request, recipients);
    }

    @Transactional
    public NoticeSendResponse sendPartnerNotice(PartnerAuthContext authContext, NoticeSendRequest request) {
        PartnerProfile profile = partnerRepository.findPartnerProfile(authContext.partnerId())
                .orElseThrow(() -> new PartnerNotFoundException(authContext.partnerId()));
        if (!authContext.canAccess(profile.countryScope())) {
            throw new ForbiddenCountryScopeException(profile.countryScope());
        }
        List<NoticeRecipientRecord> recipients = request.recipients().stream()
                .map(recipient -> repository.findPartnerRecipient(authContext.partnerId(), recipient)
                        .orElseThrow(() -> new ForbiddenCountryScopeException("noticeRecipient:" + recipient)))
                .toList();
        return createNotice("PARTNER", profile.userId(), authContext.partnerId(), "PARTNER", request, recipients);
    }

    private NoticeSendResponse createNotice(
            String senderType,
            long senderUserId,
            long senderPartnerId,
            String actorRole,
            NoticeSendRequest request,
            List<NoticeRecipientRecord> recipients
    ) {
        validateRequest(request);
        Instant now = Instant.now(clock);
        boolean scheduled = request.scheduledAt() != null;
        if (scheduled && !request.scheduledAt().isAfter(now)) {
            throw new IllegalArgumentException("scheduledAt must be in the future");
        }
        String noticeStatus = scheduled ? "SCHEDULED" : "SENT";
        String deliveryStatus = scheduled ? "PENDING" : "SENT";
        Instant deliveredAt = scheduled ? null : now;
        long noticeId = repository.createNotice(senderType, senderUserId, senderPartnerId, request, noticeStatus, now);
        repository.createRecipients(noticeId, recipients, deliveryStatus, deliveredAt);
        repository.recordActivity(actorRole, actionType(noticeStatus), "SUCCESS", noticeId, request.requestId());
        return new NoticeSendResponse(
                noticeId,
                noticeStatus,
                request.scheduledAt(),
                scheduled ? null : now,
                recipients.size(),
                actionType(noticeStatus),
                scheduled ? "notice.send.scheduled" : "notice.send.sent"
        );
    }

    private void validateRequest(NoticeSendRequest request) {
        if (!List.of("IN_APP", "EMAIL", "SMS", "PUSH").contains(request.channel())) {
            throw new IllegalArgumentException("unsupported notice channel: " + request.channel());
        }
        if (request.recipients().isEmpty()) {
            throw new IllegalArgumentException("recipients must not be empty");
        }
    }

    private String actionType(String noticeStatus) {
        return "SCHEDULED".equals(noticeStatus) ? "NOTICE_SEND_SCHEDULED" : "NOTICE_SENT";
    }
}
