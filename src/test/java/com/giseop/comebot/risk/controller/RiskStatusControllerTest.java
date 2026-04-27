package com.giseop.comebot.risk.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.config.TradingProperties;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RiskStatusController.class)
class RiskStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradingProperties tradingProperties;

    @Test
    void statusReturnsRiskPolicyProperties() throws Exception {
        org.mockito.Mockito.when(tradingProperties.getMaxOrderAmount())
                .thenReturn(new BigDecimal("100000"));
        org.mockito.Mockito.when(tradingProperties.getAllowedMarkets())
                .thenReturn(List.of("KRW-BTC", "KRW-ETH"));

        mockMvc.perform(get("/api/risk/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxOrderAmount").value(100000))
                .andExpect(jsonPath("$.allowedMarkets[0]").value("KRW-BTC"))
                .andExpect(jsonPath("$.allowedMarkets[1]").value("KRW-ETH"));
    }

    @Test
    void statusDoesNotExposeSensitiveValues() throws Exception {
        org.mockito.Mockito.when(tradingProperties.getMaxOrderAmount())
                .thenReturn(new BigDecimal("100000"));
        org.mockito.Mockito.when(tradingProperties.getAllowedMarkets())
                .thenReturn(List.of("KRW-BTC", "KRW-ETH"));

        mockMvc.perform(get("/api/risk/status"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("bot-token"))))
                .andExpect(content().string(not(containsString("chat-id"))))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("secret"))));
    }
}
