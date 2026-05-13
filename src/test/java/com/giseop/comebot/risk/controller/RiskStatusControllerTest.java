package com.giseop.comebot.risk.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.risk.ConcentrationRiskProperties;
import com.giseop.comebot.risk.DailyRiskProperties;
import com.giseop.comebot.risk.PositionExitProperties;
import com.giseop.comebot.risk.StopLossCooldownProperties;
import java.math.BigDecimal;
import java.time.Duration;
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

    @MockitoBean
    private PositionExitProperties positionExitProperties;

    @MockitoBean
    private DailyRiskProperties dailyRiskProperties;

    @MockitoBean
    private ConcentrationRiskProperties concentrationRiskProperties;

    @MockitoBean
    private StopLossCooldownProperties stopLossCooldownProperties;

    @Test
    void statusReturnsRiskPolicyProperties() throws Exception {
        org.mockito.Mockito.when(tradingProperties.getMaxOrderAmount())
                .thenReturn(new BigDecimal("100000"));
        org.mockito.Mockito.when(tradingProperties.getAllowedMarkets())
                .thenReturn(List.of("KRW-BTC", "KRW-ETH"));
        org.mockito.Mockito.when(positionExitProperties.getTakeProfitRate())
                .thenReturn(new BigDecimal("5"));
        org.mockito.Mockito.when(positionExitProperties.getStopLossRate())
                .thenReturn(new BigDecimal("-3"));
        org.mockito.Mockito.when(positionExitProperties.isPositionExitEnabled())
                .thenReturn(false);
        org.mockito.Mockito.when(dailyRiskProperties.isDailyRiskEnabled())
                .thenReturn(false);
        org.mockito.Mockito.when(dailyRiskProperties.getDailyOrderLimit())
                .thenReturn(10);
        org.mockito.Mockito.when(dailyRiskProperties.getDailyLossLimit())
                .thenReturn(new BigDecimal("50000"));
        org.mockito.Mockito.when(concentrationRiskProperties.isEnabled())
                .thenReturn(false);
        org.mockito.Mockito.when(concentrationRiskProperties.warningExposureRate(ExchangeMode.UPBIT))
                .thenReturn(new BigDecimal("7"));
        org.mockito.Mockito.when(concentrationRiskProperties.blockExposureRate(ExchangeMode.UPBIT))
                .thenReturn(new BigDecimal("10"));
        mockStopLossCooldown(false);

        mockMvc.perform(get("/api/risk/status?exchange=upbit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxOrderAmount").value(100000))
                .andExpect(jsonPath("$.allowedMarkets[0]").value("KRW-BTC"))
                .andExpect(jsonPath("$.allowedMarkets[1]").value("KRW-ETH"))
                .andExpect(jsonPath("$.takeProfitRate").value(5))
                .andExpect(jsonPath("$.stopLossRate").value(-3))
                .andExpect(jsonPath("$.positionExitEnabled").value(false))
                .andExpect(jsonPath("$.dailyRiskEnabled").value(false))
                .andExpect(jsonPath("$.dailyOrderLimit").value(10))
                .andExpect(jsonPath("$.dailyLossLimit").value(50000))
                .andExpect(jsonPath("$.concentration.exchange").value("UPBIT"))
                .andExpect(jsonPath("$.concentration.enabled").value(false))
                .andExpect(jsonPath("$.concentration.warningExposureRate").value(7))
                .andExpect(jsonPath("$.concentration.blockExposureRate").value(10))
                .andExpect(jsonPath("$.stopLossCooldown.enabled").value(false))
                .andExpect(jsonPath("$.stopLossCooldown.window").value("PT168H"))
                .andExpect(jsonPath("$.stopLossCooldown.triggerCount").value(2))
                .andExpect(jsonPath("$.stopLossCooldown.duration").value("PT24H"));
    }

    @Test
    void statusReturnsExchangeSpecificConcentrationThresholds() throws Exception {
        org.mockito.Mockito.when(tradingProperties.getMaxOrderAmount())
                .thenReturn(new BigDecimal("100000"));
        org.mockito.Mockito.when(tradingProperties.getAllowedMarkets())
                .thenReturn(List.of("ALL_USDT"));
        org.mockito.Mockito.when(positionExitProperties.getTakeProfitRate())
                .thenReturn(new BigDecimal("1.5"));
        org.mockito.Mockito.when(positionExitProperties.getStopLossRate())
                .thenReturn(new BigDecimal("-0.7"));
        org.mockito.Mockito.when(positionExitProperties.isPositionExitEnabled())
                .thenReturn(true);
        org.mockito.Mockito.when(dailyRiskProperties.getDailyLossLimit())
                .thenReturn(new BigDecimal("50000"));
        org.mockito.Mockito.when(concentrationRiskProperties.isEnabled())
                .thenReturn(true);
        org.mockito.Mockito.when(concentrationRiskProperties.warningExposureRate(ExchangeMode.BINANCE))
                .thenReturn(new BigDecimal("25"));
        org.mockito.Mockito.when(concentrationRiskProperties.blockExposureRate(ExchangeMode.BINANCE))
                .thenReturn(new BigDecimal("40"));
        mockStopLossCooldown(true);

        mockMvc.perform(get("/api/risk/status?exchange=binance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concentration.exchange").value("BINANCE"))
                .andExpect(jsonPath("$.concentration.enabled").value(true))
                .andExpect(jsonPath("$.concentration.warningExposureRate").value(25))
                .andExpect(jsonPath("$.concentration.blockExposureRate").value(40))
                .andExpect(jsonPath("$.stopLossCooldown.enabled").value(true));
    }

    @Test
    void statusDoesNotExposeSensitiveValues() throws Exception {
        org.mockito.Mockito.when(tradingProperties.getMaxOrderAmount())
                .thenReturn(new BigDecimal("100000"));
        org.mockito.Mockito.when(tradingProperties.getAllowedMarkets())
                .thenReturn(List.of("KRW-BTC", "KRW-ETH"));
        org.mockito.Mockito.when(positionExitProperties.getTakeProfitRate())
                .thenReturn(new BigDecimal("5"));
        org.mockito.Mockito.when(positionExitProperties.getStopLossRate())
                .thenReturn(new BigDecimal("-3"));
        org.mockito.Mockito.when(positionExitProperties.isPositionExitEnabled())
                .thenReturn(false);
        org.mockito.Mockito.when(dailyRiskProperties.isDailyRiskEnabled())
                .thenReturn(false);
        org.mockito.Mockito.when(dailyRiskProperties.getDailyOrderLimit())
                .thenReturn(10);
        org.mockito.Mockito.when(dailyRiskProperties.getDailyLossLimit())
                .thenReturn(new BigDecimal("50000"));
        org.mockito.Mockito.when(concentrationRiskProperties.warningExposureRate(ExchangeMode.UPBIT))
                .thenReturn(new BigDecimal("7"));
        org.mockito.Mockito.when(concentrationRiskProperties.blockExposureRate(ExchangeMode.UPBIT))
                .thenReturn(new BigDecimal("10"));
        mockStopLossCooldown(false);

        mockMvc.perform(get("/api/risk/status"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("bot-token"))))
                .andExpect(content().string(not(containsString("chat-id"))))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("secret"))));
    }

    private void mockStopLossCooldown(boolean enabled) {
        org.mockito.Mockito.when(stopLossCooldownProperties.isEnabled()).thenReturn(enabled);
        org.mockito.Mockito.when(stopLossCooldownProperties.getWindow()).thenReturn(Duration.ofDays(7));
        org.mockito.Mockito.when(stopLossCooldownProperties.getTriggerCount()).thenReturn(2);
        org.mockito.Mockito.when(stopLossCooldownProperties.getDuration()).thenReturn(Duration.ofHours(24));
    }
}
