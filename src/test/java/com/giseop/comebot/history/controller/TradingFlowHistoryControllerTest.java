package com.giseop.comebot.history.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.strategy.domain.SignalType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TradingFlowHistoryController.class)
class TradingFlowHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradingFlowHistoryService tradingFlowHistoryService;

    @Test
    void findRecentReturnsHistoryList() throws Exception {
        when(tradingFlowHistoryService.findRecent(20))
                .thenReturn(List.of(history("history-1")));

        mockMvc.perform(get("/api/trading-flow/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("history-1"))
                .andExpect(jsonPath("$[0].market").value("KRW-BTC"))
                .andExpect(jsonPath("$[0].currentPrice").value(100))
                .andExpect(jsonPath("$[0].orderStatus").value("FILLED"));
    }

    @Test
    void findByIdReturnsHistory() throws Exception {
        when(tradingFlowHistoryService.findById("history-1"))
                .thenReturn(Optional.of(history("history-1")));

        mockMvc.perform(get("/api/trading-flow/history/history-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("history-1"))
                .andExpect(jsonPath("$.signalType").value("BUY"));
    }

    @Test
    void findByIdReturnsNotFoundForMissingHistory() throws Exception {
        when(tradingFlowHistoryService.findById("missing"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/trading-flow/history/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findRecentReturnsBadRequestWhenLimitIsZeroOrLess() throws Exception {
        mockMvc.perform(get("/api/trading-flow/history").param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    private TradingFlowHistory history(String id) {
        return new TradingFlowHistory(
                id,
                "KRW-BTC",
                new BigDecimal("100"),
                SignalType.BUY,
                "Test threshold buy signal",
                true,
                OrderStatus.FILLED,
                "Paper trading order filled",
                Instant.parse("2026-04-27T00:00:00Z")
        );
    }
}
