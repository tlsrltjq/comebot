package com.giseop.comebot.scheduler.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.scheduler.CandidateSchedulerProperties;
import com.giseop.comebot.scheduler.PositionExitSchedulerProperties;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SchedulerStatusController.class)
class SchedulerStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradingSchedulerProperties tradingSchedulerProperties;

    @MockitoBean
    private CandidateSchedulerProperties candidateSchedulerProperties;

    @MockitoBean
    private PositionExitSchedulerProperties positionExitSchedulerProperties;

    @MockitoBean
    private PaperPortfolioService paperPortfolioService;

    @Test
    void getStatusReturnsOk() throws Exception {
        schedulerProperties(false, 60000, List.of("KRW-BTC", "KRW-ETH"));
        candidateSchedulerProperties(false, 60000, List.of("KRW-BTC", "KRW-ETH"));
        exitSchedulerProperties(true, 5000, false, ExchangeMode.UPBIT);

        mockMvc.perform(get("/api/scheduler/status"))
                .andExpect(status().isOk());
    }

    @Test
    void getStatusReturnsSchedulerFields() throws Exception {
        schedulerProperties(false, 60000, List.of("KRW-BTC", "KRW-ETH"));
        candidateSchedulerProperties(false, 60000, List.of("KRW-BTC", "KRW-ETH"));
        exitSchedulerProperties(true, 5000, false, ExchangeMode.UPBIT);
        when(paperPortfolioService.findPositions(ExchangeMode.UPBIT))
                .thenReturn(List.of(new PaperPosition("KRW-BTC", new BigDecimal("0.1"), new BigDecimal("100"))));

        mockMvc.perform(get("/api/scheduler/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.fixedDelayMs").value(60000))
                .andExpect(jsonPath("$.markets[0]").value("KRW-BTC"))
                .andExpect(jsonPath("$.markets[1]").value("KRW-ETH"))
                .andExpect(jsonPath("$.candidateEnabled").value(false))
                .andExpect(jsonPath("$.candidateFixedDelayMs").value(60000))
                .andExpect(jsonPath("$.candidateMarkets[0]").value("KRW-BTC"))
                .andExpect(jsonPath("$.candidateMarkets[1]").value("KRW-ETH"))
                .andExpect(jsonPath("$.candidateNotifySummary").value(false))
                .andExpect(jsonPath("$.candidateExchange").value("UPBIT"))
                .andExpect(jsonPath("$.exitEnabled").value(true))
                .andExpect(jsonPath("$.exitFixedDelayMs").value(5000))
                .andExpect(jsonPath("$.exitSaveHoldHistory").value(false))
                .andExpect(jsonPath("$.exitExchange").value("UPBIT"))
                .andExpect(jsonPath("$.exitPositionMarketCount").value(1));
    }

    @Test
    void getStatusReturnsDefaultDisabledValue() throws Exception {
        schedulerProperties(false, 60000, List.of("KRW-BTC", "KRW-ETH"));
        candidateSchedulerProperties(false, 60000, List.of("KRW-BTC", "KRW-ETH"));
        exitSchedulerProperties(true, 5000, false, ExchangeMode.UPBIT);

        mockMvc.perform(get("/api/scheduler/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.candidateEnabled").value(false));
    }

    @Test
    void getStatusReturnsConfiguredCandidateSchedulerFields() throws Exception {
        schedulerProperties(true, 30000, List.of("KRW-BTC", "KRW-ETH"));
        candidateSchedulerProperties(true, 45000, List.of("KRW-XRP"));
        exitSchedulerProperties(true, 7000, true, ExchangeMode.UPBIT);

        mockMvc.perform(get("/api/scheduler/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markets[0]").value("KRW-BTC"))
                .andExpect(jsonPath("$.candidateEnabled").value(true))
                .andExpect(jsonPath("$.candidateFixedDelayMs").value(45000))
                .andExpect(jsonPath("$.candidateMarkets[0]").value("KRW-XRP"))
                .andExpect(jsonPath("$.exitFixedDelayMs").value(7000))
                .andExpect(jsonPath("$.exitSaveHoldHistory").value(true));
    }

    private void schedulerProperties(boolean enabled, long fixedDelayMs, List<String> markets) {
        when(tradingSchedulerProperties.isEnabled()).thenReturn(enabled);
        when(tradingSchedulerProperties.getFixedDelayMs()).thenReturn(fixedDelayMs);
        when(tradingSchedulerProperties.getMarkets()).thenReturn(markets);
    }

    private void candidateSchedulerProperties(boolean enabled, long fixedDelayMs, List<String> markets) {
        when(candidateSchedulerProperties.isEnabled()).thenReturn(enabled);
        when(candidateSchedulerProperties.getFixedDelayMs()).thenReturn(fixedDelayMs);
        when(candidateSchedulerProperties.getMarkets()).thenReturn(markets);
        when(candidateSchedulerProperties.isNotifySummary()).thenReturn(false);
        when(candidateSchedulerProperties.getExchange()).thenReturn(ExchangeMode.UPBIT);
    }

    private void exitSchedulerProperties(boolean enabled, long fixedDelayMs, boolean saveHoldHistory, ExchangeMode exchange) {
        when(positionExitSchedulerProperties.isEnabled()).thenReturn(enabled);
        when(positionExitSchedulerProperties.getFixedDelayMs()).thenReturn(fixedDelayMs);
        when(positionExitSchedulerProperties.isSaveHoldHistory()).thenReturn(saveHoldHistory);
        when(positionExitSchedulerProperties.getExchange()).thenReturn(exchange);
        when(paperPortfolioService.findPositions(exchange)).thenReturn(List.of());
    }
}
