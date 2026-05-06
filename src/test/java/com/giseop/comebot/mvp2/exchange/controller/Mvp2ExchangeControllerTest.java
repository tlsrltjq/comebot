package com.giseop.comebot.mvp2.exchange.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.mvp2.exchange.Exchange;
import com.giseop.comebot.mvp2.exchange.ExchangeMarketDataProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = Mvp2ExchangeController.class)
@Import(Mvp2ExchangeControllerTest.TestConfig.class)
class Mvp2ExchangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exchangesReturnsEnabledMvp2Exchanges() throws Exception {
        mockMvc.perform(get("/api/mvp2/exchanges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].exchange").value("BINANCE"))
                .andExpect(jsonPath("$[0].displayName").value("Binance"))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[0].publicMarketDataOnly").value(true))
                .andExpect(jsonPath("$[0].statusPath").value("/api/mvp2/exchanges/BINANCE/status"))
                .andExpect(jsonPath("$[1].exchange").value("UPBIT"));
    }

    @Test
    void exchangeStatusReturnsPublicDataOnlyState() throws Exception {
        mockMvc.perform(get("/api/mvp2/exchanges/binance/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exchange").value("BINANCE"))
                .andExpect(jsonPath("$.displayName").value("Binance"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.publicMarketDataOnly").value(true))
                .andExpect(jsonPath("$.realTradingSupported").value(false))
                .andExpect(jsonPath("$.marketData").value(containsString("Binance public")))
                .andExpect(jsonPath("$.message").value(containsString("PAPER/SIMULATION")));
    }

    @Test
    void exchangeStatusDoesNotExposeSensitiveValues() throws Exception {
        mockMvc.perform(get("/api/mvp2/exchanges/upbit/status"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Access Key"))))
                .andExpect(content().string(not(containsString("Secret Key"))))
                .andExpect(content().string(not(containsString("api-key"))))
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void unknownExchangeReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/mvp2/exchanges/unknown/status"))
                .andExpect(status().isNotFound());
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        ExchangeMarketDataProvider upbitProvider() {
            return provider(Exchange.UPBIT);
        }

        @Bean
        ExchangeMarketDataProvider binanceProvider() {
            return provider(Exchange.BINANCE);
        }

        private ExchangeMarketDataProvider provider(Exchange exchange) {
            ExchangeMarketDataProvider provider = org.mockito.Mockito.mock(ExchangeMarketDataProvider.class);
            when(provider.exchange()).thenReturn(exchange);
            when(provider.getTickers(List.of())).thenReturn(List.of());
            return provider;
        }
    }
}
