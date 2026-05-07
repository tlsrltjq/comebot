package com.giseop.comebot.portfolio.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.portfolio.domain.PaperPortfolio;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.dto.PortfolioValuationResponse;
import com.giseop.comebot.portfolio.dto.PositionValuationResponse;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.portfolio.service.PaperPortfolioValuationService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PortfolioStatusController.class)
class PortfolioStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaperPortfolioService paperPortfolioService;

    @MockitoBean
    private PaperPortfolioValuationService paperPortfolioValuationService;

    @Test
    void statusReturnsPortfolioStatus() throws Exception {
        when(paperPortfolioService.getPortfolio()).thenReturn(new PaperPortfolio(
                new BigDecimal("999900"),
                new BigDecimal("20"),
                List.of()
        ));

        mockMvc.perform(get("/api/portfolio/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cash").value(999900))
                .andExpect(jsonPath("$.realizedProfit").value(20));
    }

    @Test
    void statusAcceptsLowercaseUpbitExchange() throws Exception {
        when(paperPortfolioService.getPortfolio()).thenReturn(new PaperPortfolio(
                new BigDecimal("999900"),
                new BigDecimal("20"),
                List.of()
        ));

        mockMvc.perform(get("/api/portfolio/status").param("exchange", "upbit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cash").value(999900));
    }

    @Test
    void statusReturnsNotImplementedForBinanceExchange() throws Exception {
        mockMvc.perform(get("/api/portfolio/status").param("exchange", "binance"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void statusReturnsBadRequestForUnknownExchange() throws Exception {
        mockMvc.perform(get("/api/portfolio/status").param("exchange", "coinbase"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void positionsReturnsPositions() throws Exception {
        when(paperPortfolioService.findPositions()).thenReturn(List.of(
                new PaperPosition("KRW-BTC", new BigDecimal("1.5"), new BigDecimal("100"))
        ));

        mockMvc.perform(get("/api/portfolio/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].market").value("KRW-BTC"))
                .andExpect(jsonPath("$[0].quantity").value(1.5))
                .andExpect(jsonPath("$[0].averageBuyPrice").value(100));
    }

    @Test
    void positionsReturnsNotImplementedForBinanceExchange() throws Exception {
        mockMvc.perform(get("/api/portfolio/positions").param("exchange", "binance"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void valuationReturnsPortfolioValuation() throws Exception {
        when(paperPortfolioValuationService.valuate()).thenReturn(new PortfolioValuationResponse(
                new BigDecimal("999900"),
                new BigDecimal("150"),
                new BigDecimal("1000050"),
                new BigDecimal("20"),
                new BigDecimal("50"),
                new BigDecimal("70"),
                List.of(new PositionValuationResponse(
                        "KRW-BTC",
                        new BigDecimal("1"),
                        new BigDecimal("100"),
                        new BigDecimal("150"),
                        new BigDecimal("150"),
                        new BigDecimal("50"),
                        new BigDecimal("50.00000000")
                ))
        ));

        mockMvc.perform(get("/api/portfolio/valuation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cash").value(999900))
                .andExpect(jsonPath("$.totalPositionValue").value(150))
                .andExpect(jsonPath("$.totalEquity").value(1000050))
                .andExpect(jsonPath("$.realizedProfit").value(20))
                .andExpect(jsonPath("$.unrealizedProfit").value(50))
                .andExpect(jsonPath("$.totalProfit").value(70))
                .andExpect(jsonPath("$.positions[0].market").value("KRW-BTC"))
                .andExpect(jsonPath("$.positions[0].currentPrice").value(150))
                .andExpect(jsonPath("$.positions[0].unrealizedProfitRate").value(50.00000000));
    }

    @Test
    void valuationReturnsBadGatewayWhenCurrentPriceLookupFails() throws Exception {
        when(paperPortfolioValuationService.valuate()).thenThrow(new IllegalStateException("failed"));

        mockMvc.perform(get("/api/portfolio/valuation"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Portfolio valuation failed"));
    }
}
