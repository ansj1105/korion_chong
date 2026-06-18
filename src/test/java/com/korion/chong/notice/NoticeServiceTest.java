package com.korion.chong.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.korion.chong.leader.AuthContext;
import com.korion.chong.leader.ForbiddenCountryScopeException;
import com.korion.chong.leader.LeaderDashboardRepository;
import com.korion.chong.leader.LeaderProfile;
import com.korion.chong.partner.PartnerAuthContext;
import com.korion.chong.partner.PartnerDashboardRepository;
import com.korion.chong.partner.PartnerProfile;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NoticeServiceTest {
    private final NoticeRepository repository = Mockito.mock(NoticeRepository.class);
    private final LeaderDashboardRepository leaderRepository = Mockito.mock(LeaderDashboardRepository.class);
    private final PartnerDashboardRepository partnerRepository = Mockito.mock(PartnerDashboardRepository.class);
    private final NoticeService service = new NoticeService(
            repository,
            leaderRepository,
            partnerRepository,
            Clock.fixed(Instant.parse("2026-06-19T00:00:00Z"), ZoneOffset.UTC)
    );

    @BeforeEach
    void setUp() {
        Mockito.when(leaderRepository.findLeaderProfile(10L))
                .thenReturn(Optional.of(new LeaderProfile(10L, 100L, "leader.kr", "COUNTRY_LEADER_APPROVED", List.of("KR"))));
        Mockito.when(partnerRepository.findPartnerProfile(20L))
                .thenReturn(Optional.of(new PartnerProfile(20L, 200L, "partner.kr", "SALES_PARTNER_APPROVED", "KR", "Seoul", "Seoul")));
        Mockito.when(repository.createNotice(Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any()))
                .thenReturn(501L);
    }

    @Test
    void immediateLeaderNoticeStoresSentDeliveryHistory() {
        NoticeRecipientRequest partner = new NoticeRecipientRequest("PARTNER", 21L, null);
        NoticeRecipientRequest merchant = new NoticeRecipientRequest("MERCHANT", null, 31L);
        Mockito.when(repository.findLeaderRecipient(10L, partner))
                .thenReturn(Optional.of(new NoticeRecipientRecord("PARTNER", 201L, 21L, null)));
        Mockito.when(repository.findLeaderRecipient(10L, merchant))
                .thenReturn(Optional.of(new NoticeRecipientRecord("MERCHANT", 301L, null, 31L)));

        NoticeSendResponse response = service.sendLeaderNotice(
                new AuthContext(10L, java.util.Set.of("KR")),
                request(null, List.of(partner, merchant))
        );

        assertThat(response.status()).isEqualTo("SENT");
        assertThat(response.sentAt()).isEqualTo(Instant.parse("2026-06-19T00:00:00Z"));
        assertThat(response.recipientCount()).isEqualTo(2);
        Mockito.verify(repository).createRecipients(
                501L,
                List.of(
                        new NoticeRecipientRecord("PARTNER", 201L, 21L, null),
                        new NoticeRecipientRecord("MERCHANT", 301L, null, 31L)
                ),
                "SENT",
                Instant.parse("2026-06-19T00:00:00Z")
        );
        Mockito.verify(repository).recordActivity("LEADER", "NOTICE_SENT", "SUCCESS", 501L, "req-notice");
    }

    @Test
    void scheduledPartnerNoticeStoresPendingDeliveryHistory() {
        Instant scheduledAt = Instant.parse("2026-06-20T00:00:00Z");
        NoticeRecipientRequest merchant = new NoticeRecipientRequest("MERCHANT", null, 31L);
        Mockito.when(repository.findPartnerRecipient(20L, merchant))
                .thenReturn(Optional.of(new NoticeRecipientRecord("MERCHANT", 301L, null, 31L)));

        NoticeSendResponse response = service.sendPartnerNotice(
                new PartnerAuthContext(20L, java.util.Set.of("KR")),
                request(scheduledAt, List.of(merchant))
        );

        assertThat(response.status()).isEqualTo("SCHEDULED");
        assertThat(response.scheduledAt()).isEqualTo(scheduledAt);
        assertThat(response.sentAt()).isNull();
        Mockito.verify(repository).createRecipients(
                501L,
                List.of(new NoticeRecipientRecord("MERCHANT", 301L, null, 31L)),
                "PENDING",
                null
        );
        Mockito.verify(repository).recordActivity("PARTNER", "NOTICE_SEND_SCHEDULED", "SUCCESS", 501L, "req-notice");
    }

    @Test
    void partnerCannotSendToForeignMerchant() {
        NoticeRecipientRequest foreignMerchant = new NoticeRecipientRequest("MERCHANT", null, 99L);
        Mockito.when(repository.findPartnerRecipient(20L, foreignMerchant))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendPartnerNotice(
                new PartnerAuthContext(20L, java.util.Set.of("KR")),
                request(null, List.of(foreignMerchant))
        )).isInstanceOf(ForbiddenCountryScopeException.class);
    }

    @Test
    void pastScheduledAtIsRejected() {
        NoticeRecipientRequest merchant = new NoticeRecipientRequest("MERCHANT", null, 31L);
        Mockito.when(repository.findPartnerRecipient(20L, merchant))
                .thenReturn(Optional.of(new NoticeRecipientRecord("MERCHANT", 301L, null, 31L)));

        assertThatThrownBy(() -> service.sendPartnerNotice(
                new PartnerAuthContext(20L, java.util.Set.of("KR")),
                request(Instant.parse("2026-06-18T00:00:00Z"), List.of(merchant))
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    private NoticeSendRequest request(Instant scheduledAt, List<NoticeRecipientRequest> recipients) {
        return new NoticeSendRequest(
                "Notice",
                "OPERATIONS",
                "Body",
                "IN_APP",
                scheduledAt,
                recipients,
                "req-notice"
        );
    }
}
