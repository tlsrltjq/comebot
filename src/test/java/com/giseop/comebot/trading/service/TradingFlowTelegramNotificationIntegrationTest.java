package com.giseop.comebot.trading.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.gateway.PaperTradingExecutionGateway;
import com.giseop.comebot.execution.service.OrderExecutionService;
import com.giseop.comebot.history.repository.InMemoryTradingFlowHistoryRepository;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.market.provider.InMemoryMarketPriceProvider;
import com.giseop.comebot.notification.NotificationPolicyService;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.notification.TradingFlowNotificationService;
import com.giseop.comebot.portfolio.PaperPortfolioProperties;
import com.giseop.comebot.portfolio.repository.InMemoryPaperPortfolioRepository;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.risk.service.RiskValidationService;
import com.giseop.comebot.strategy.service.OrderRequestFactory;
import com.giseop.comebot.strategy.service.SimpleThresholdStrategy;
import com.giseop.comebot.telegram.TelegramProperties;
import com.giseop.comebot.telegram.sender.TelegramApiClient;
import com.giseop.comebot.telegram.sender.TelegramNotificationSender;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

class TradingFlowTelegramNotificationIntegrationTest {

    private InMemoryMarketPriceProvider marketPriceProvider;
    private InMemoryTradingFlowHistoryRepository historyRepository;
    private NotificationProperties notificationProperties;
    private TelegramProperties telegramProperties;
    private RecordingTelegramApiClient telegramApiClient;
    private TradingFlowService tradingFlowService;

    @BeforeEach
    void setUp() {
        StrategyProperties strategyProperties = new StrategyProperties();
        strategyProperties.setBuyPrice(new BigDecimal("100"));
        strategyProperties.setSellPrice(new BigDecimal("200"));
        strategyProperties.setOrderQuantity(new BigDecimal("1"));

        TradingProperties tradingProperties = new TradingProperties();
        tradingProperties.setMaxOrderAmount(new BigDecimal("100000"));
        tradingProperties.setAllowedMarkets(List.of("KRW-BTC", "KRW-ETH"));

        marketPriceProvider = new InMemoryMarketPriceProvider();
        historyRepository = new InMemoryTradingFlowHistoryRepository();
        notificationProperties = new NotificationProperties();
        telegramProperties = new TelegramProperties();
        telegramApiClient = new RecordingTelegramApiClient();

        tradingFlowService = new TradingFlowService(
                marketPriceProvider,
                new SimpleThresholdStrategy(strategyProperties),
                new OrderRequestFactory(),
                new OrderExecutionService(
                        new PaperTradingExecutionGateway(),
                        new RiskValidationService(tradingProperties),
                        paperPortfolioService()
                ),
                new TradingFlowHistoryService(historyRepository),
                notificationProperties,
                new NotificationPolicyService(notificationProperties),
                new TradingFlowNotificationService(new TelegramNotificationSender(telegramProperties, telegramApiClient))
        );
    }

    @Test
    void runDoesNotSendTelegramWhenNotificationIsDisabled() {
        configureTelegram(true, "token", "chat-id");
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("100"));

        tradingFlowService.run("KRW-BTC");

        assertThat(telegramApiClient.callCount).isZero();
    }

    @Test
    void runDoesNotSendTelegramWhenTelegramIsDisabled() {
        notificationProperties.setEnabled(true);
        configureTelegram(false, "token", "chat-id");
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("100"));

        tradingFlowService.run("KRW-BTC");

        assertThat(telegramApiClient.callCount).isZero();
    }

    @Test
    void runSendsTelegramWhenNotificationAndTelegramAreEnabledAndConfigured() {
        notificationProperties.setEnabled(true);
        configureTelegram(true, "token", "chat-id");
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("100"));

        tradingFlowService.run("KRW-BTC");

        assertThat(telegramApiClient.callCount).isEqualTo(1);
        assertThat(telegramApiClient.botToken).isEqualTo("token");
        assertThat(telegramApiClient.chatId).isEqualTo("chat-id");
    }

    @Test
    void runKeepsResultAndHistoryWhenTelegramSendFails() {
        notificationProperties.setEnabled(true);
        configureTelegram(true, "token", "chat-id");
        telegramApiClient.fail = true;
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("100"));

        TradingFlowResult result = tradingFlowService.run("KRW-BTC");

        assertThat(result.orderStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(historyRepository.findRecent(1)).hasSize(1);
        assertThat(historyRepository.findRecent(1).getFirst().orderStatus()).isEqualTo(OrderStatus.FILLED);
    }

    private void configureTelegram(boolean enabled, String botToken, String chatId) {
        telegramProperties.setEnabled(enabled);
        telegramProperties.setBotToken(botToken);
        telegramProperties.setChatId(chatId);
    }

    private PaperPortfolioService paperPortfolioService() {
        PaperPortfolioProperties properties = new PaperPortfolioProperties();
        properties.setInitialCash(new BigDecimal("1000000"));
        InMemoryPaperPortfolioRepository repository = new InMemoryPaperPortfolioRepository();
        PaperPortfolioService service = new PaperPortfolioService(repository, properties);
        service.initialize();
        return service;
    }

    private static class RecordingTelegramApiClient implements TelegramApiClient {

        private int callCount;
        private boolean fail;
        private String botToken;
        private String chatId;

        @Override
        public void sendMessage(String botToken, String chatId, String text) {
            callCount++;
            this.botToken = botToken;
            this.chatId = chatId;
            if (fail) {
                throw new RestClientException("failed") {
                };
            }
        }
    }
}
