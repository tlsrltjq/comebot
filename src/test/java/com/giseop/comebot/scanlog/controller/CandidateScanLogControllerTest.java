package com.giseop.comebot.scanlog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.scanlog.domain.CandidateScanLog;
import com.giseop.comebot.scanlog.service.CandidateScanLogService;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CandidateScanLogController.class)
class CandidateScanLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CandidateScanLogService candidateScanLogService;

    @Test
    void findSinceReturnsLogsAsJson() throws Exception {
        when(candidateScanLogService.findSince(eq(ExchangeMode.UPBIT), any(Instant.class), eq(null)))
                .thenReturn(List.of(scanLog("KRW-BTC", CandidateDecision.SELECTED)));

        mockMvc.perform(get("/api/candidate-scan-log").param("range", "1h").param("exchange", "UPBIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].market").value("KRW-BTC"))
                .andExpect(jsonPath("$[0].decision").value("SELECTED"));
    }

    @Test
    void findSinceWithDecisionFilterPassesItToService() throws Exception {
        when(candidateScanLogService.findSince(eq(ExchangeMode.UPBIT), any(Instant.class), eq(CandidateDecision.SKIPPED)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/candidate-scan-log")
                        .param("range", "24h")
                        .param("exchange", "UPBIT")
                        .param("decision", "SKIPPED"))
                .andExpect(status().isOk());

        verify(candidateScanLogService).findSince(eq(ExchangeMode.UPBIT), any(Instant.class), eq(CandidateDecision.SKIPPED));
    }

    @Test
    void findSinceDefaultsToBinanceExchangeWhenSpecified() throws Exception {
        when(candidateScanLogService.findSince(eq(ExchangeMode.BINANCE), any(Instant.class), eq(null)))
                .thenReturn(List.of(scanLog("BTCUSDT", CandidateDecision.SKIPPED)));

        mockMvc.perform(get("/api/candidate-scan-log").param("exchange", "BINANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].market").value("BTCUSDT"));
    }

    @Test
    void findSinceReturnsEmptyListWhenNoLogs() throws Exception {
        when(candidateScanLogService.findSince(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/candidate-scan-log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    private CandidateScanLog scanLog(String market, CandidateDecision decision) {
        return new CandidateScanLog(
                "test-id",
                ExchangeMode.UPBIT,
                market,
                decision,
                "reason",
                new BigDecimal("100"),
                new BigDecimal("2.5"),
                new BigDecimal("5"),
                new BigDecimal("30"),
                MarketTrend.UP,
                decision == CandidateDecision.SELECTED,
                Instant.parse("2026-05-01T00:00:00Z")
        );
    }
}
