package com.giseop.comebot.strategy.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.strategy.candidate.CandidateDecision;
import com.giseop.comebot.strategy.candidate.CandidateExecutionService;
import com.giseop.comebot.strategy.candidate.CandidateScanCache;
import com.giseop.comebot.strategy.candidate.CandidateScannerService;
import com.giseop.comebot.strategy.candidate.TradingCandidate;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.trading.service.TradingFlowResult;

@WebMvcTest(CandidateController.class)
@Import(CandidateScanCache.class)
class CandidateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CandidateScannerService candidateScannerService;

    @MockitoBean
    private CandidateExecutionService candidateExecutionService;

    @Autowired
    private CandidateScanCache candidateScanCache;

    @BeforeEach
    void clearCache() {
        candidateScanCache.clear();
    }

    @Test
    void getCandidatesReturnsAllowedMarketScanResults() throws Exception {
        when(candidateScannerService.scanAllowedMarkets(ExchangeMode.UPBIT, 20)).thenReturn(List.of(
                candidate("KRW-BTC", CandidateDecision.SELECTED),
                candidate("KRW-ETH", CandidateDecision.SKIPPED)
        ));

        mockMvc.perform(get("/api/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].market").value("KRW-BTC"))
                .andExpect(jsonPath("$[0].decision").value("SELECTED"))
                .andExpect(jsonPath("$[0].reasonType").value("SELECTED"))
                .andExpect(jsonPath("$[0].riskReasonType").value("NONE"))
                .andExpect(jsonPath("$[0].trend").value("UP"))
                .andExpect(jsonPath("$[1].market").value("KRW-ETH"))
                .andExpect(jsonPath("$[1].decision").value("SKIPPED"))
                .andExpect(jsonPath("$[1].reasonType").value("TREND_NOT_UP"))
                .andExpect(jsonPath("$[1].riskReasonType").value("NONE"));
    }

    @Test
    void getCandidatesWithMarketScansSingleMarket() throws Exception {
        when(candidateScannerService.scan(ExchangeMode.UPBIT, "KRW-BTC"))
                .thenReturn(candidate("KRW-BTC", CandidateDecision.SELECTED));

        mockMvc.perform(get("/api/candidates").param("market", "KRW-BTC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].market").value("KRW-BTC"))
                .andExpect(jsonPath("$[0].decision").value("SELECTED"));

        verify(candidateScannerService).scan(ExchangeMode.UPBIT, "KRW-BTC");
        verify(candidateScannerService, never()).scanAllowedMarkets(any(), anyInt());
    }

    @Test
    void getCandidatesAcceptsLowercaseUpbitExchange() throws Exception {
        when(candidateScannerService.scanAllowedMarkets(ExchangeMode.UPBIT, 20)).thenReturn(List.of(
                candidate("KRW-BTC", CandidateDecision.SELECTED)
        ));

        mockMvc.perform(get("/api/candidates").param("exchange", "upbit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].market").value("KRW-BTC"));
    }

    @Test
    void getCandidatesAcceptsBinanceExchange() throws Exception {
        when(candidateScannerService.scanAllowedMarkets(ExchangeMode.BINANCE, 20)).thenReturn(List.of(
                candidate("BTCUSDT", CandidateDecision.SELECTED)
        ));

        mockMvc.perform(get("/api/candidates").param("exchange", "binance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].market").value("BTCUSDT"));

        verify(candidateScannerService).scanAllowedMarkets(ExchangeMode.BINANCE, 20);
    }

    @Test
    void getCandidatesReturnsBadRequestForUnknownExchange() throws Exception {
        mockMvc.perform(get("/api/candidates").param("exchange", "coinbase"))
                .andExpect(status().isBadRequest());

        verify(candidateScannerService, never()).scanAllowedMarkets(any(), anyInt());
    }

    @Test
    void blankMarketReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/candidates").param("market", " "))
                .andExpect(status().isBadRequest());

        verify(candidateScannerService, never()).scan(" ");
        verify(candidateScannerService, never()).scanAllowedMarkets(any(), anyInt());
    }

    @Test
    void executeCandidateRunsCandidateExecutionService() throws Exception {
        when(candidateExecutionService.execute("KRW-BTC"))
                .thenReturn(new TradingFlowResult(
                        "KRW-BTC",
                        new BigDecimal("100"),
                        SignalType.BUY,
                        "Volatility long candidate selected",
                        true,
                        OrderStatus.FILLED,
                        "Paper trading order filled",
                        Instant.parse("2026-04-30T00:00:00Z")
                ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/candidates/execute")
                        .param("market", "KRW-BTC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.market").value("KRW-BTC"))
                .andExpect(jsonPath("$.signalType").value("BUY"))
                .andExpect(jsonPath("$.orderCreated").value(true))
                .andExpect(jsonPath("$.orderStatus").value("FILLED"));
    }

    @Test
    void blankMarketExecutionReturnsBadRequest() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/candidates/execute")
                        .param("market", " "))
                .andExpect(status().isBadRequest());

        verify(candidateExecutionService, never()).execute(" ");
    }


    @Test
    void responseDoesNotExposeSensitiveValues() throws Exception {
        when(candidateScannerService.scanAllowedMarkets(ExchangeMode.UPBIT, 20)).thenReturn(List.of(
                candidate("KRW-BTC", CandidateDecision.SELECTED)
        ));

        mockMvc.perform(get("/api/candidates"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("token"))))
                .andExpect(content().string(not(containsString("chat-id"))))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("secret"))));
    }

    @Test
    void responseClassifiesRiskReasonWithoutParsingOnClient() throws Exception {
        when(candidateScannerService.scanAllowedMarkets(ExchangeMode.UPBIT, 20)).thenReturn(List.of(
                riskCandidate("KRW-XRP", "Market concentration exceeds block exposure rate: market=KRW-XRP exposure=10% limit=10%"),
                riskCandidate("KRW-ETH", "Stop loss cooldown active: market=KRW-ETH stopLossCount=2 cooldownUntil=2026-05-13T00:00:00Z")
        ));

        mockMvc.perform(get("/api/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reasonType").value("CONCENTRATION_RISK"))
                .andExpect(jsonPath("$[0].riskReasonType").value("CONCENTRATION"))
                .andExpect(jsonPath("$[1].reasonType").value("STOP_LOSS_COOLDOWN"))
                .andExpect(jsonPath("$[1].riskReasonType").value("STOP_LOSS_COOLDOWN"));
    }

    @Test
    void fullCandidateScanUsesShortLivedCache() throws Exception {
        when(candidateScannerService.scanAllowedMarkets(ExchangeMode.UPBIT, 20)).thenReturn(List.of(
                candidate("KRW-BTC", CandidateDecision.SELECTED)
        ));

        mockMvc.perform(get("/api/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].market").value("KRW-BTC"));
        mockMvc.perform(get("/api/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].market").value("KRW-BTC"));

        verify(candidateScannerService, times(1)).scanAllowedMarkets(ExchangeMode.UPBIT, 20);
    }

    @Test
    void refreshBypassesFullCandidateScanCache() throws Exception {
        when(candidateScannerService.scanAllowedMarkets(ExchangeMode.UPBIT, 20))
                .thenReturn(List.of(candidate("KRW-BTC", CandidateDecision.SELECTED)))
                .thenReturn(List.of(candidate("KRW-ETH", CandidateDecision.SKIPPED)));

        mockMvc.perform(get("/api/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].market").value("KRW-BTC"));
        mockMvc.perform(get("/api/candidates").param("refresh", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].market").value("KRW-ETH"));

        verify(candidateScannerService, times(2)).scanAllowedMarkets(ExchangeMode.UPBIT, 20);
    }

    @Test
    void fullCandidateScanAcceptsLimitParameter() throws Exception {
        when(candidateScannerService.scanAllowedMarkets(ExchangeMode.UPBIT, 5)).thenReturn(List.of(
                candidate("KRW-BTC", CandidateDecision.SELECTED)
        ));

        mockMvc.perform(get("/api/candidates").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].market").value("KRW-BTC"));

        verify(candidateScannerService).scanAllowedMarkets(ExchangeMode.UPBIT, 5);
    }

    private TradingCandidate candidate(String market, CandidateDecision decision) {
        return new TradingCandidate(
                market,
                decision,
                decision == CandidateDecision.SELECTED ? "Volatility long candidate selected" : "Trend is not UP",
                new BigDecimal("100"),
                new BigDecimal("2.5"),
                new BigDecimal("5.0"),
                new BigDecimal("10.0"),
                MarketTrend.UP,
                Instant.parse("2026-04-30T00:00:00Z")
        );
    }

    private TradingCandidate riskCandidate(String market, String reason) {
        return new TradingCandidate(
                market,
                CandidateDecision.SKIPPED,
                reason,
                new BigDecimal("100"),
                new BigDecimal("2.5"),
                new BigDecimal("5.0"),
                new BigDecimal("10.0"),
                MarketTrend.UP,
                Instant.parse("2026-04-30T00:00:00Z")
        );
    }
}
