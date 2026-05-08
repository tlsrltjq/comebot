package com.giseop.comebot.system.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.config.StrategySelectionProperties;
import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.database.DatabaseHealthResult;
import com.giseop.comebot.database.DatabaseHealthService;
import com.giseop.comebot.market.provider.MarketPriceProviderProperties;
import com.giseop.comebot.market.provider.MarketPriceProviderType;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.scheduler.CandidateSchedulerProperties;
import com.giseop.comebot.scheduler.PositionExitSchedulerProperties;
import com.giseop.comebot.safety.SafetyProperties;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import com.giseop.comebot.telegram.TelegramProperties;
import com.giseop.comebot.telegram.inbound.TelegramInboundProperties;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SystemStatusController.class)
class SystemStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DatabaseHealthService databaseHealthService;

    @MockitoBean
    private MarketPriceProviderProperties marketPriceProviderProperties;

    @MockitoBean
    private StrategyProperties strategyProperties;

    @MockitoBean
    private StrategySelectionProperties strategySelectionProperties;

    @MockitoBean
    private TradingProperties tradingProperties;

    @MockitoBean
    private TradingSchedulerProperties tradingSchedulerProperties;

    @MockitoBean
    private CandidateSchedulerProperties candidateSchedulerProperties;

    @MockitoBean
    private PositionExitSchedulerProperties positionExitSchedulerProperties;

    @MockitoBean
    private PaperPortfolioService paperPortfolioService;

    @MockitoBean
    private SafetyProperties safetyProperties;

    @MockitoBean
    private NotificationProperties notificationProperties;

    @MockitoBean
    private TelegramProperties telegramProperties;

    @MockitoBean
    private TelegramInboundProperties telegramInboundProperties;

    @Test
    void statusReturnsAllMainSections() throws Exception {
        setUpStatus(true);

        mockMvc.perform(get("/api/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.database.connected").value(true))
                .andExpect(jsonPath("$.marketProvider.provider").value("UPBIT"))
                .andExpect(jsonPath("$.marketProvider.externalProvider").value(true))
                .andExpect(jsonPath("$.strategy.strategyName").value("SimpleThresholdStrategy"))
                .andExpect(jsonPath("$.strategy.buyPrice").value(90000000))
                .andExpect(jsonPath("$.strategy.sellPrice").value(110000000))
                .andExpect(jsonPath("$.strategy.orderQuantity").value(0.001))
                .andExpect(jsonPath("$.strategy.orderAmount").value(10000))
                .andExpect(jsonPath("$.risk.maxOrderAmount").value(100000))
                .andExpect(jsonPath("$.risk.allowedMarkets[0]").value("KRW-BTC"))
                .andExpect(jsonPath("$.scheduler.enabled").value(false))
                .andExpect(jsonPath("$.scheduler.fixedDelayMs").value(60000))
                .andExpect(jsonPath("$.scheduler.markets[1]").value("KRW-ETH"))
                .andExpect(jsonPath("$.scheduler.candidateEnabled").value(false))
                .andExpect(jsonPath("$.scheduler.candidateFixedDelayMs").value(60000))
                .andExpect(jsonPath("$.scheduler.candidateMarkets[1]").value("KRW-ETH"))
                .andExpect(jsonPath("$.scheduler.candidateNotifySummary").value(false))
                .andExpect(jsonPath("$.scheduler.candidateExchange").value("UPBIT"))
                .andExpect(jsonPath("$.scheduler.exitEnabled").value(true))
                .andExpect(jsonPath("$.scheduler.exitFixedDelayMs").value(5000))
                .andExpect(jsonPath("$.scheduler.exitPositionMarketCount").value(1))
                .andExpect(jsonPath("$.safety.killSwitchEnabled").value(false))
                .andExpect(jsonPath("$.notification.enabled").value(false))
                .andExpect(jsonPath("$.notification.sendHold").value(false))
                .andExpect(jsonPath("$.notification.sendFilled").value(true))
                .andExpect(jsonPath("$.notification.sendRejected").value(true))
                .andExpect(jsonPath("$.telegram.enabled").value(true))
                .andExpect(jsonPath("$.telegram.configured").value(true))
                .andExpect(jsonPath("$.telegram.inboundEnabled").value(true))
                .andExpect(jsonPath("$.telegram.manualPaperExecutionEnabled").value(false));
    }

    @Test
    void statusDoesNotExposeSensitiveValues() throws Exception {
        setUpStatus(true);

        mockMvc.perform(get("/api/system/status"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("bot-token"))))
                .andExpect(content().string(not(containsString("chat-id"))))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("secret"))));
    }

    @Test
    void statusReturnsOkWhenDatabaseIsDisconnected() throws Exception {
        setUpStatus(false);

        mockMvc.perform(get("/api/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.database.connected").value(false));
    }

    @Test
    void statusAcceptsLowercaseUpbitExchange() throws Exception {
        setUpStatus(true);

        mockMvc.perform(get("/api/system/status").param("exchange", "upbit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketProvider.provider").value("UPBIT"));
    }

    @Test
    void statusAcceptsBinanceExchange() throws Exception {
        setUpStatus(true);

        mockMvc.perform(get("/api/system/status").param("exchange", "binance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketProvider.provider").value("UPBIT"));
    }

    @Test
    void statusReturnsBadRequestForUnknownExchange() throws Exception {
        mockMvc.perform(get("/api/system/status").param("exchange", "coinbase"))
                .andExpect(status().isBadRequest());
    }

    private void setUpStatus(boolean databaseConnected) {
        org.mockito.Mockito.when(databaseHealthService.check())
                .thenReturn(new DatabaseHealthResult(databaseConnected, "PostgreSQL"));
        org.mockito.Mockito.when(marketPriceProviderProperties.getPriceProvider())
                .thenReturn(MarketPriceProviderType.UPBIT);
        org.mockito.Mockito.when(strategyProperties.getBuyPrice())
                .thenReturn(new BigDecimal("90000000"));
        org.mockito.Mockito.when(strategyProperties.getSellPrice())
                .thenReturn(new BigDecimal("110000000"));
        org.mockito.Mockito.when(strategyProperties.getOrderQuantity())
                .thenReturn(new BigDecimal("0.001"));
        org.mockito.Mockito.when(strategyProperties.getOrderAmount())
                .thenReturn(new BigDecimal("10000"));
        org.mockito.Mockito.when(strategySelectionProperties.getStrategyName())
                .thenReturn("SimpleThresholdStrategy");
        org.mockito.Mockito.when(tradingProperties.getMaxOrderAmount())
                .thenReturn(new BigDecimal("100000"));
        org.mockito.Mockito.when(tradingProperties.getAllowedMarkets())
                .thenReturn(List.of("KRW-BTC", "KRW-ETH"));
        org.mockito.Mockito.when(tradingSchedulerProperties.isEnabled())
                .thenReturn(false);
        org.mockito.Mockito.when(tradingSchedulerProperties.getFixedDelayMs())
                .thenReturn(60000L);
        org.mockito.Mockito.when(tradingSchedulerProperties.getMarkets())
                .thenReturn(List.of("KRW-BTC", "KRW-ETH"));
        org.mockito.Mockito.when(candidateSchedulerProperties.isEnabled())
                .thenReturn(false);
        org.mockito.Mockito.when(candidateSchedulerProperties.getFixedDelayMs())
                .thenReturn(60000L);
        org.mockito.Mockito.when(candidateSchedulerProperties.getMarkets())
                .thenReturn(List.of("KRW-BTC", "KRW-ETH"));
        org.mockito.Mockito.when(candidateSchedulerProperties.isNotifySummary())
                .thenReturn(false);
        org.mockito.Mockito.when(candidateSchedulerProperties.getExchange())
                .thenReturn(ExchangeMode.UPBIT);
        org.mockito.Mockito.when(positionExitSchedulerProperties.isEnabled())
                .thenReturn(true);
        org.mockito.Mockito.when(positionExitSchedulerProperties.getFixedDelayMs())
                .thenReturn(5000L);
        org.mockito.Mockito.when(positionExitSchedulerProperties.isSaveHoldHistory())
                .thenReturn(false);
        org.mockito.Mockito.when(positionExitSchedulerProperties.getExchange())
                .thenReturn(ExchangeMode.UPBIT);
        org.mockito.Mockito.when(paperPortfolioService.findPositions(ExchangeMode.UPBIT))
                .thenReturn(List.of(new PaperPosition("KRW-BTC", new BigDecimal("0.1"), new BigDecimal("90000000"))));
        org.mockito.Mockito.when(safetyProperties.isKillSwitchEnabled())
                .thenReturn(false);
        org.mockito.Mockito.when(notificationProperties.isEnabled())
                .thenReturn(false);
        org.mockito.Mockito.when(notificationProperties.isSendHold())
                .thenReturn(false);
        org.mockito.Mockito.when(notificationProperties.isSendFilled())
                .thenReturn(true);
        org.mockito.Mockito.when(notificationProperties.isSendRejected())
                .thenReturn(true);
        org.mockito.Mockito.when(telegramProperties.isEnabled())
                .thenReturn(true);
        org.mockito.Mockito.when(telegramProperties.isConfigured())
                .thenReturn(true);
        org.mockito.Mockito.when(telegramInboundProperties.isEnabled())
                .thenReturn(true);
    }
}
