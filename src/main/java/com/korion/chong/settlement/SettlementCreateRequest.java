package com.korion.chong.settlement;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SettlementCreateRequest(
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        @NotNull @DecimalMin(value = "0.000000000000000001", inclusive = true) BigDecimal requestedAmount,
        Long walletAddressId,
        @Size(max = 2000) String memo,
        @Size(max = 128) String requestId
) {
}
