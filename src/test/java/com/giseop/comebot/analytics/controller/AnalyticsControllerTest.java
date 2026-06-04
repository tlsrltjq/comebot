package com.giseop.comebot.analytics.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.analytics.dto.AnalyticsLossResponse;
import com.giseop.comebot.analytics.dto.AnalyticsPnlResponse;
import com.giseop.comebot.analytics.dto.AnalyticsRange;
import com.giseop.comebot.analytics.dto.AnalyticsSummaryResponse;
import com.giseop.comebot.analytics.service.AnalyticsService;
import com.giseop.comebot.analytics.service.MatchedTradeService;
import com.giseop.comebot.exchange.ExchangeMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @MockitoBean
    private MatchedTradeService matchedTradeService;

    @Test
    void summaryReturnsAnalyticsSummary() throws Exception {
        when(analyticsService.summary(any(AnalyticsRange.class), any(ExchangeMode.class))).thenReturn(new AnalyticsSummaryResponse(
                "24h",
                Instant.parse("2026-05-03T00:00:00Z"),
                Instant.parse("2026-05-04T00:00:00Z"),
                10,
                2,
                3,
                5,
                4,
                1,
                0,
                2,
                1,
                new BigDecimal("-1.2"),
                new BigDecimal("2.1"),
                new BigDecimal("50.00000000"),
                3600,
                new BigDecimal("1.75000000"),
                List.of(),
                List.of()
        ));

        mockMvc.perform(get("/api/analytics/summary").param("range", "24h"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.range").value("24h"))
                .andExpect(jsonPath("$.buyCount").value(2))
                .andExpect(jsonPath("$.stopLossCount").value(2))
                .andExpect(jsonPath("$.winRate").value(50.00000000))
                .andExpect(jsonPath("$.averageHoldingSeconds").value(3600))
                .andExpect(jsonPath("$.profitLossRatio").value(1.75000000));
    }

    @Test
    void summaryPassesExchangeMode() throws Exception {
        when(analyticsService.summary(any(AnalyticsRange.class), any(ExchangeMode.class))).thenReturn(new AnalyticsSummaryResponse(
                "1h",
                Instant.parse("2026-05-03T23:00:00Z"),
                Instant.parse("2026-05-04T00:00:00Z"),
                1,
                1,
                0,
                0,
                1,
                0,
                0,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                BigDecimal.ZERO,
                List.of(),
                List.of()
        ));

        mockMvc.perform(get("/api/analytics/summary").param("range", "1h").param("exchange", "binance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buyCount").value(1));

        verify(analyticsService).summary(AnalyticsRange.ONE_HOUR, ExchangeMode.BINANCE);
    }

    @Test
    void pnlReturnsPortfolioProfitSummary() throws Exception {
        when(analyticsService.pnl(any(AnalyticsRange.class), any(ExchangeMode.class))).thenReturn(new AnalyticsPnlResponse(
                "1h",
                Instant.parse("2026-05-03T23:00:00Z"),
                Instant.parse("2026-05-04T00:00:00Z"),
                new BigDecimal("900000"),
                new BigDecimal("120000"),
                new BigDecimal("1020000"),
                new BigDecimal("10000"),
                new BigDecimal("10000"),
                new BigDecimal("20000"),
                2
        ));

        mockMvc.perform(get("/api/analytics/pnl").param("range", "1h"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProfit").value(20000))
                .andExpect(jsonPath("$.positionCount").value(2));
    }

    @Test
    void lossesReturnsLossSummary() throws Exception {
        when(analyticsService.losses(any(AnalyticsRange.class), any(ExchangeMode.class))).thenReturn(new AnalyticsLossResponse("24h", List.of(), List.of()));

        mockMvc.perform(get("/api/analytics/losses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.range").value("24h"));
    }
}
