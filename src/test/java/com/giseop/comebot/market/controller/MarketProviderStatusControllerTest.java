package com.giseop.comebot.market.controller;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.market.provider.MarketPriceProviderProperties;
import com.giseop.comebot.market.provider.MarketPriceProviderType;
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

    @Test
    void statusReturnsInMemoryProviderByDefault() throws Exception {
        org.mockito.Mockito.when(marketPriceProviderProperties.getPriceProvider())
                .thenReturn(MarketPriceProviderType.IN_MEMORY);

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

        mockMvc.perform(get("/api/market-provider/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("BINANCE"))
                .andExpect(jsonPath("$.externalProvider").value(true))
                .andExpect(jsonPath("$.message").value(containsString("Binance public spot ticker API")));
    }

    @Test
    void statusDoesNotExposeSensitiveValues() throws Exception {
        org.mockito.Mockito.when(marketPriceProviderProperties.getPriceProvider())
                .thenReturn(MarketPriceProviderType.UPBIT);

        mockMvc.perform(get("/api/market-provider/status"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Access Key"))))
                .andExpect(content().string(not(containsString("Secret Key"))))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("bot-token"))));
    }
}
