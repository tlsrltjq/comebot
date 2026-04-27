package com.giseop.comebot.scheduler.dto;

import java.util.List;

public record SchedulerStatusResponse(
        boolean enabled,
        long fixedDelayMs,
        List<String> markets
) {
}
