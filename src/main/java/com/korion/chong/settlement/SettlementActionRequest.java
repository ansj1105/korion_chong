package com.korion.chong.settlement;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SettlementActionRequest(
        @DecimalMin(value = "0.000000000000000001", inclusive = true) BigDecimal amount,
        @Size(max = 2000) String reason,
        @Size(max = 128) String requestId
) {
}
