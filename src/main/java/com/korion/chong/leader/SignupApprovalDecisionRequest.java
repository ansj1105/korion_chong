package com.korion.chong.leader;

import jakarta.validation.constraints.Size;

public record SignupApprovalDecisionRequest(
        @Size(max = 2000) String reason,
        @Size(max = 128) String requestId
) {
}
