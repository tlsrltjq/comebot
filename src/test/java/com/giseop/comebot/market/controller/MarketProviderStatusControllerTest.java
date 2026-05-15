package com.giseop.comebot.market.controller;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.service.MarketDataReadiness;
import com.giseop.comebot.market.service.MarketDataReadinessService;
import com.giseop.comebot.market.provider.MarketPriceProviderProperties;
import com.giseop.comebot.market.provider.MarketPriceProviderType;
import com.giseop.comebot.market.service.TickerSnapshotStore;
import com.giseop.comebot.market.websocket.MarketWebSocketProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MarketProviderStatusController.class)
class MarketProviderStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MarketPriceProviderProperties marketPriceProviderProperties;

    @MockitoBean
    private MarketWebSocketProperties marketWebSocketProperties;

    @MockitoBean
    private TickerSnapshotStore tickerSnapshotStore;

    @MockitoBean
    private MarketDataReadinessService marketDataReadinessService;

    @Test
    void statusReturnsInMemoryProviderByDefault() throws Exception {
        org.mockito.Mockito.when(marketPriceProviderProperties.getPriceProvider())
                .thenReturn(MarketPriceProviderType.IN_MEMORY);
        readyReadiness();

        mockMvc.perform(get("/api/market-provider/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("IN_MEMORY"))
                .andExpect(jsonPath("$.externalProvider").value(false))
                .andExpect(jsonPath("$.message").value("Using in-memory test market prices."));
    }

    @Test
    void statusReturnsUpbitProviderWhenConfigured() throws Exception {
        org.mockito.Mockito.when(marketPriceProviderProperties.getPriceProvider())
                .thenReturn(MarketPriceProviderType.UPBIT);
        readyReadiness();

        mockMvc.perform(get("/api/market-provider/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("UPBIT"))
                .andExpect(jsonPath("$.externalProvider").value(true))
                .andExpect(jsonPath("$.message").value(containsString("Upbit public ticker API")));
    }

    @Test
    void statusReturnsBinanceProviderWhenConfigured() throws Exception {
        org.mockito.Mockito.when(marketPriceProviderProperties.getPriceProvider())
                .thenReturn(MarketPriceProviderType.BINANCE);
        readyReadiness();

        mockMvc.perform(get("/api/market-provider/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("BINANCE"))
                .andExpect(jsonPath("$.externalProvider").value(true))
                .andExpect(jsonPath("$.message").value(containsString("Binance public spot ticker API")));
    }

    @Test
    void statusReturnsSnapshotProviderWithWebSocketCounts() throws Exception {
        org.mockito.Mockito.when(marketPriceProviderProperties.getPriceProvider())
                .thenReturn(MarketPriceProviderType.SNAPSHOT);
        org.mockito.Mockito.when(marketWebSocketProperties.isEnabled())
                .thenReturn(true);
        org.mockito.Mockito.when(tickerSnapshotStore.count())
                .thenReturn(3);
        org.mockito.Mockito.when(marketWebSocketProperties.orderStaleDuration())
                .thenReturn(java.time.Duration.ofMillis(3000));
        org.mockito.Mockito.when(marketWebSocketProperties.getOrderStaleMs())
                .thenReturn(3000L);
        org.mockito.Mockito.when(tickerSnapshotStore.countFresh(
                        org.mockito.Mockito.eq(java.time.Duration.ofMillis(3000)),
                        org.mockito.Mockito.any(java.time.Instant.class)
                ))
                .thenReturn(2);
        org.mockito.Mockito.when(tickerSnapshotStore.countFresh(
                        org.mockito.Mockito.eq(ExchangeMode.UPBIT),
                        org.mockito.Mockito.eq(java.time.Duration.ofMillis(3000)),
                        org.mockito.Mockito.any(java.time.Instant.class)
                ))
                .thenReturn(1);
        org.mockito.Mockito.when(tickerSnapshotStore.countFresh(
                        org.mockito.Mockito.eq(ExchangeMode.BINANCE),
                        org.mockito.Mockito.eq(java.time.Duration.ofMillis(3000)),
                        org.mockito.Mockito.any(java.time.Instant.class)
                ))
                .thenReturn(1);
        readyReadiness();

        mockMvc.perform(get("/api/market-provider/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("SNAPSHOT"))
                .andExpect(jsonPath("$.externalProvider").value(true))
                .andExpect(jsonPath("$.webSocketEnabled").value(true))
                .andExpect(jsonPath("$.snapshotCount").value(3))
                .andExpect(jsonPath("$.upbitFreshSnapshotCount").value(1))
                .andExpect(jsonPath("$.binanceFreshSnapshotCount").value(1))
                .andExpect(jsonPath("$.freshSnapshotCount").value(2))
                .andExpect(jsonPath("$.staleSnapshotCount").value(1))
                .andExpect(jsonPath("$.orderStaleMs").value(3000))
                .andExpect(jsonPath("$.automationReady").value(true));
    }

    @Test
    void statusShowsAutomationBlockedWhenNoExchangeHasFreshSnapshot() throws Exception {
        org.mockito.Mockito.when(marketPriceProviderProperties.getPriceProvider())
                .thenReturn(MarketPriceProviderType.SNAPSHOT);
        org.mockito.Mockito.when(marketWebSocketProperties.isEnabled())
                .thenReturn(true);
        org.mockito.Mockito.when(marketWebSocketProperties.orderStaleDuration())
                .thenReturn(java.time.Duration.ofMillis(3000));
        org.mockito.Mockito.when(marketDataReadinessService.readiness(ExchangeMode.UPBIT))
                .thenReturn(MarketDataReadiness.snapshot(ExchangeMode.UPBIT, 0, 0));
        org.mockito.Mockito.when(marketDataReadinessService.readiness(ExchangeMode.BINANCE))
                .thenReturn(MarketDataReadiness.snapshot(ExchangeMode.BINANCE, 0, 0));

        mockMvc.perform(get("/api/market-provider/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.automationReady").value(false))
                .andExpect(jsonPath("$.automationBlockReason").value("Fresh ticker snapshot is not available for any exchange"));
    }


    @Test
    void statusDoesNotExposeSensitiveValues() throws Exception {
        org.mockito.Mockito.when(marketPriceProviderProperties.getPriceProvider())
                .thenReturn(MarketPriceProviderType.UPBIT);
        readyReadiness();

        mockMvc.perform(get("/api/market-provider/status"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Access Key"))))
                .andExpect(content().string(not(containsString("Secret Key"))))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("bot-token"))));
    }

    private void readyReadiness() {
        org.mockito.Mockito.when(marketDataReadinessService.readiness(ExchangeMode.UPBIT))
                .thenReturn(MarketDataReadiness.ready(ExchangeMode.UPBIT, "ready"));
        org.mockito.Mockito.when(marketDataReadinessService.readiness(ExchangeMode.BINANCE))
                .thenReturn(MarketDataReadiness.ready(ExchangeMode.BINANCE, "ready"));
    }
}
