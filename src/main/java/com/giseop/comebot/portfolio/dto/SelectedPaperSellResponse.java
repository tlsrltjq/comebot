package com.giseop.comebot.portfolio.dto;

import java.util.List;

public record SelectedPaperSellResponse(
        String exchange,
        int requestedCount,
        int succeededCount,
        int failedCount,
        List<SelectedPaperSellResultResponse> results
) {
}
