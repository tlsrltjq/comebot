package com.giseop.comebot.trading.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.gateway.PaperTradingExecutionGateway;
import com.giseop.comebot.execution.service.OrderExecutionService;
import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.history.repository.InMemoryTradingFlowHistoryRepository;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.market.provider.InMemoryMarketPriceProvider;
import com.giseop.comebot.notification.NotificationPolicyService;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.notification.TradingFlowNotificationService;
import com.giseop.comebot.risk.service.RiskValidationService;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.strategy.service.OrderRequestFactory;
import com.giseop.comebot.strategy.service.SimpleThresholdStrategy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TradingFlowServiceTest {

    private InMemoryMarketPriceProvider marketPriceProvider;
    private InMemoryTradingFlowHistoryRepository historyRepository;
    private RecordingTradingFlowNotificationService notificationService;
    private NotificationProperties notificationProperties;
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
        notificationService = new RecordingTradingFlowNotificationService();
        tradingFlowService = new TradingFlowService(
                marketPriceProvider,
                new SimpleThresholdStrategy(strategyProperties),
                new OrderRequestFactory(),
                new OrderExecutionService(
                        new PaperTradingExecutionGateway(),
                        new RiskValidationService(tradingProperties)
                ),
                new TradingFlowHistoryService(historyRepository),
                notificationProperties,
                new NotificationPolicyService(notificationProperties),
                notificationService
        );
    }

    @Test
    void runFillsPaperBuyOrderWhenPriceIsAtOrBelowBuyThreshold() {
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("100"));

        TradingFlowResult result = tradingFlowService.run("KRW-BTC");

        assertThat(result.signalType()).isEqualTo(SignalType.BUY);
        assertThat(result.orderCreated()).isTrue();
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(historyRepository.findRecent(1)).hasSize(1);
        assertThat(notificationService.results).isEmpty();
    }

    @Test
    void runFillsPaperSellOrderWhenPriceIsAtOrAboveSellThreshold() {
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("200"));

        TradingFlowResult result = tradingFlowService.run("KRW-BTC");

        assertThat(result.signalType()).isEqualTo(SignalType.SELL);
        assertThat(result.orderCreated()).isTrue();
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void runDoesNotExecuteOrderWhenSignalIsHold() {
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("150"));

        TradingFlowResult result = tradingFlowService.run("KRW-BTC");

        assertThat(result.signalType()).isEqualTo(SignalType.HOLD);
        assertThat(result.orderCreated()).isFalse();
        assertThat(result.orderStatus()).isNull();
        assertThat(result.message()).isEqualTo("No order created");

        TradingFlowHistory history = historyRepository.findRecent(1).getFirst();
        assertThat(history.signalType()).isEqualTo(SignalType.HOLD);
        assertThat(history.orderCreated()).isFalse();
    }

    @Test
    void runReturnsRejectedOrderResultWhenMarketIsNotAllowed() {
        marketPriceProvider.updatePrice("KRW-XRP", new BigDecimal("100"));

        TradingFlowResult result = tradingFlowService.run("KRW-XRP");

        assertThat(result.signalType()).isEqualTo(SignalType.BUY);
        assertThat(result.orderCreated()).isTrue();
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(result.message()).isEqualTo("Market is not allowed");

        TradingFlowHistory history = historyRepository.findRecent(1).getFirst();
        assertThat(history.orderStatus()).isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void runNotifiesWhenNotificationIsEnabled() {
        notificationProperties.setEnabled(true);
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("100"));

        TradingFlowResult result = tradingFlowService.run("KRW-BTC");

        assertThat(notificationService.results).containsExactly(result);
    }

    @Test
    void runDoesNotNotifyHoldByDefault() {
        notificationProperties.setEnabled(true);
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("150"));

        tradingFlowService.run("KRW-BTC");

        assertThat(notificationService.results).isEmpty();
        assertThat(historyRepository.findRecent(1)).hasSize(1);
    }

    @Test
    void runNotifiesHoldWhenSendHoldIsTrue() {
        notificationProperties.setEnabled(true);
        notificationProperties.setSendHold(true);
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("150"));

        TradingFlowResult result = tradingFlowService.run("KRW-BTC");

        assertThat(result.signalType()).isEqualTo(SignalType.HOLD);
        assertThat(notificationService.results).containsExactly(result);
    }

    @Test
    void runNotifiesFilledWhenSendFilledIsTrue() {
        notificationProperties.setEnabled(true);
        notificationProperties.setSendFilled(true);
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("100"));

        TradingFlowResult result = tradingFlowService.run("KRW-BTC");

        assertThat(result.orderStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(notificationService.results).containsExactly(result);
    }

    @Test
    void runDoesNotNotifyFilledWhenSendFilledIsFalse() {
        notificationProperties.setEnabled(true);
        notificationProperties.setSendFilled(false);
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("100"));

        TradingFlowResult result = tradingFlowService.run("KRW-BTC");

        assertThat(result.orderStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(notificationService.results).isEmpty();
        assertThat(historyRepository.findRecent(1)).hasSize(1);
    }

    @Test
    void runNotifiesRejectedWhenSendRejectedIsTrue() {
        notificationProperties.setEnabled(true);
        notificationProperties.setSendRejected(true);
        marketPriceProvider.updatePrice("KRW-XRP", new BigDecimal("100"));

        TradingFlowResult result = tradingFlowService.run("KRW-XRP");

        assertThat(result.orderStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(notificationService.results).containsExactly(result);
    }

    @Test
    void runDoesNotNotifyRejectedWhenSendRejectedIsFalse() {
        notificationProperties.setEnabled(true);
        notificationProperties.setSendRejected(false);
        marketPriceProvider.updatePrice("KRW-XRP", new BigDecimal("100"));

        TradingFlowResult result = tradingFlowService.run("KRW-XRP");

        assertThat(result.orderStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(notificationService.results).isEmpty();
        assertThat(historyRepository.findRecent(1)).hasSize(1);
    }

    @Test
    void runReturnsResultWhenNotificationFails() {
        notificationProperties.setEnabled(true);
        notificationService.fail = true;
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("100"));

        TradingFlowResult result = tradingFlowService.run("KRW-BTC");

        assertThat(result.orderStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void runStoresHistoryWhenNotificationFails() {
        notificationProperties.setEnabled(true);
        notificationService.fail = true;
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("100"));

        tradingFlowService.run("KRW-BTC");

        assertThat(historyRepository.findRecent(1)).hasSize(1);
        assertThat(historyRepository.findRecent(1).getFirst().orderStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void runCanNotifyHoldRejectedAndFilledResults() {
        notificationProperties.setEnabled(true);
        notificationProperties.setSendHold(true);

        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("150"));
        TradingFlowResult hold = tradingFlowService.run("KRW-BTC");

        marketPriceProvider.updatePrice("KRW-XRP", new BigDecimal("100"));
        TradingFlowResult rejected = tradingFlowService.run("KRW-XRP");

        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("100"));
        TradingFlowResult filled = tradingFlowService.run("KRW-BTC");

        assertThat(hold.signalType()).isEqualTo(SignalType.HOLD);
        assertThat(rejected.orderStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(filled.orderStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(notificationService.results).containsExactly(hold, rejected, filled);
    }

    private static class RecordingTradingFlowNotificationService extends TradingFlowNotificationService {

        private final List<TradingFlowResult> results = new ArrayList<>();
        private boolean fail;

        private RecordingTradingFlowNotificationService() {
            super(message -> {
            });
        }

        @Override
        public void notify(TradingFlowResult result) {
            if (fail) {
                throw new IllegalStateException("notification failed");
            }
            results.add(result);
        }
    }
}
