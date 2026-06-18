package com.korion.chong.notice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record NoticeSendRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 64) String noticeType,
        @NotBlank String body,
        @NotBlank @Size(max = 32) String channel,
        Instant scheduledAt,
        @NotEmpty @Valid List<NoticeRecipientRequest> recipients,
        @Size(max = 128) String requestId
) {
}
