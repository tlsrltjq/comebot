package com.giseop.comebot.trading.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.trading.service.TradingFlowResult;
import com.giseop.comebot.trading.service.TradingFlowService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TradingFlowController.class)
class TradingFlowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradingFlowService tradingFlowService;

    @Test
    void runReturnsOkForMarket() throws Exception {
        when(tradingFlowService.run("KRW-BTC"))
                .thenReturn(filledResult());

        mockMvc.perform(get("/api/trading-flow/run").param("market", "KRW-BTC"))
                .andExpect(status().isOk());

        verify(tradingFlowService).run("KRW-BTC");
    }

    @Test
    void runReturnsResponseFields() throws Exception {
        when(tradingFlowService.run("KRW-BTC"))
                .thenReturn(filledResult());

        mockMvc.perform(get("/api/trading-flow/run").param("market", "KRW-BTC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.market").value("KRW-BTC"))
                .andExpect(jsonPath("$.signalType").value("BUY"))
                .andExpect(jsonPath("$.orderCreated").value(true))
                .andExpect(jsonPath("$.message").value("Paper trading order filled"));
    }

    @Test
    void runReturnsBadRequestWhenMarketIsBlank() throws Exception {
        mockMvc.perform(get("/api/trading-flow/run").param("market", " "))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tradingFlowService);
    }

    @Test
    void runDelegatesProcessingToTradingFlowService() throws Exception {
        when(tradingFlowService.run("KRW-BTC"))
                .thenReturn(filledResult());

        mockMvc.perform(get("/api/trading-flow/run").param("market", "KRW-BTC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("FILLED"));

        verify(tradingFlowService).run("KRW-BTC");
    }

    @Test
    void runReturnsBlockedResultWhenKillSwitchBlocksTradingFlow() throws Exception {
        when(tradingFlowService.run("KRW-BTC"))
                .thenReturn(blockedResult());

        mockMvc.perform(get("/api/trading-flow/run").param("market", "KRW-BTC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.market").value("KRW-BTC"))
                .andExpect(jsonPath("$.orderCreated").value(false))
                .andExpect(jsonPath("$.orderStatus").value("REJECTED"))
                .andExpect(jsonPath("$.message").value("Kill switch enabled: trading flow blocked"));
    }

    private TradingFlowResult filledResult() {
        return new TradingFlowResult(
                "KRW-BTC",
                new BigDecimal("50000000"),
                SignalType.BUY,
                "Test threshold buy signal",
                true,
                OrderStatus.FILLED,
                "Paper trading order filled",
                Instant.parse("2026-04-27T00:00:00Z")
        );
    }

    private TradingFlowResult blockedResult() {
        return new TradingFlowResult(
                "KRW-BTC",
                null,
                null,
                "Kill switch enabled",
                false,
                OrderStatus.REJECTED,
                "Kill switch enabled: trading flow blocked",
                Instant.parse("2026-04-27T00:00:00Z")
        );
    }
}
