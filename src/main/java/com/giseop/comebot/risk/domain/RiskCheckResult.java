package com.giseop.comebot.risk.domain;

import java.time.Instant;

public record RiskCheckResult(
        RiskDecision decision,
        String reason,
        Instant checkedAt
) {
}
