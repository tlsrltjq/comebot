package com.giseop.comebot.strategy.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.strategy.candidate.CandidateDecision;
import com.giseop.comebot.strategy.candidate.CandidateScannerService;
import com.giseop.comebot.strategy.candidate.TradingCandidate;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CandidateController.class)
class CandidateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CandidateScannerService candidateScannerService;

    @Test
    void getCandidatesReturnsAllowedMarketScanResults() throws Exception {
        when(candidateScannerService.scanAllowedMarkets()).thenReturn(List.of(
                candidate("KRW-BTC", CandidateDecision.SELECTED),
                candidate("KRW-ETH", CandidateDecision.SKIPPED)
        ));

        mockMvc.perform(get("/api/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].market").value("KRW-BTC"))
                .andExpect(jsonPath("$[0].decision").value("SELECTED"))
                .andExpect(jsonPath("$[0].trend").value("UP"))
                .andExpect(jsonPath("$[1].market").value("KRW-ETH"))
                .andExpect(jsonPath("$[1].decision").value("SKIPPED"));
    }

    @Test
    void getCandidatesWithMarketScansSingleMarket() throws Exception {
        when(candidateScannerService.scan("KRW-BTC"))
                .thenReturn(candidate("KRW-BTC", CandidateDecision.SELECTED));

        mockMvc.perform(get("/api/candidates").param("market", "KRW-BTC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].market").value("KRW-BTC"))
                .andExpect(jsonPath("$[0].decision").value("SELECTED"));

        verify(candidateScannerService).scan("KRW-BTC");
        verify(candidateScannerService, never()).scanAllowedMarkets();
    }

    @Test
    void blankMarketReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/candidates").param("market", " "))
                .andExpect(status().isBadRequest());

        verify(candidateScannerService, never()).scan(" ");
        verify(candidateScannerService, never()).scanAllowedMarkets();
    }

    @Test
    void responseDoesNotExposeSensitiveValues() throws Exception {
        when(candidateScannerService.scanAllowedMarkets()).thenReturn(List.of(
                candidate("KRW-BTC", CandidateDecision.SELECTED)
        ));

        mockMvc.perform(get("/api/candidates"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("token"))))
                .andExpect(content().string(not(containsString("chat-id"))))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("secret"))));
    }

    private TradingCandidate candidate(String market, CandidateDecision decision) {
        return new TradingCandidate(
                market,
                decision,
                decision == CandidateDecision.SELECTED ? "Volatility long candidate selected" : "Trend is not UP",
                new BigDecimal("2.5"),
                new BigDecimal("5.0"),
                new BigDecimal("10.0"),
                MarketTrend.UP,
                Instant.parse("2026-04-30T00:00:00Z")
        );
    }
}
