package com.giseop.comebot.market.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.market.provider.InMemoryMarketPriceProvider;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MarketPriceController.class)
class MarketPriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InMemoryMarketPriceProvider marketPriceProvider;

    @Test
    void getPriceReturnsCurrentTestPrice() throws Exception {
        when(marketPriceProvider.getCurrentPrice("KRW-BTC"))
                .thenReturn(marketPrice("KRW-BTC", "90000000"));

        mockMvc.perform(get("/api/market-prices/KRW-BTC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.market").value("KRW-BTC"))
                .andExpect(jsonPath("$.currentPrice").value(90000000));

        verify(marketPriceProvider).getCurrentPrice("KRW-BTC");
    }

    @Test
    void updatePriceReturnsUpdatedTestPrice() throws Exception {
        when(marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("50000000")))
                .thenReturn(marketPrice("KRW-BTC", "50000000"));

        mockMvc.perform(put("/api/market-prices/KRW-BTC")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":50000000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.market").value("KRW-BTC"))
                .andExpect(jsonPath("$.currentPrice").value(50000000));

        verify(marketPriceProvider).updatePrice("KRW-BTC", new BigDecimal("50000000"));
    }

    @Test
    void updatePriceReturnsBadRequestWhenPriceIsNull() throws Exception {
        mockMvc.perform(put("/api/market-prices/KRW-BTC")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(marketPriceProvider);
    }

    @Test
    void updatePriceReturnsBadRequestWhenPriceIsZeroOrLess() throws Exception {
        mockMvc.perform(put("/api/market-prices/KRW-BTC")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":0}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(marketPriceProvider);
    }

    private MarketPrice marketPrice(String market, String price) {
        return new MarketPrice(market, new BigDecimal(price), Instant.parse("2026-04-27T00:00:00Z"));
    }
}
