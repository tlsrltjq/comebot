package com.giseop.comebot.portfolio.dto;

import java.util.List;

public record SelectedPaperSellRequest(
        List<String> markets
) {
}
