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
import com.giseop.comebot.portfolio.PaperPortfolioProperties;
import com.giseop.comebot.portfolio.repository.InMemoryPaperPortfolioRepository;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.risk.DailyRiskProperties;
import com.giseop.comebot.risk.PositionExitProperties;
import com.giseop.comebot.risk.service.DailyRiskValidationService;
import com.giseop.comebot.risk.service.PositionExitSignalService;
import com.giseop.comebot.risk.service.RiskValidationService;
import com.giseop.comebot.safety.KillSwitchService;
import com.giseop.comebot.safety.SafetyProperties;
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
    private PaperPortfolioService paperPortfolioService;
    private PositionExitProperties positionExitProperties;
    private SafetyProperties safetyProperties;
    private TradingFlowService tradingFlowService;

    @BeforeEach
    void setUp() {
        StrategyProperties strategyProperties = new StrategyProperties();
        strategyProperties.setBuyPrice(new BigDecimal("100"));
        strategyProperties.setSellPrice(new BigDecimal("200"));
        strategyProperties.setOrderQuantity(new BigDecimal("1"));
        strategyProperties.setOrderAmount(new BigDecimal("10000"));

        TradingProperties tradingProperties = new TradingProperties();
        tradingProperties.setMaxOrderAmount(new BigDecimal("100000"));
        tradingProperties.setAllowedMarkets(List.of("KRW-BTC", "KRW-ETH"));

        marketPriceProvider = new InMemoryMarketPriceProvider();
        historyRepository = new InMemoryTradingFlowHistoryRepository();
        notificationProperties = new NotificationProperties();
        notificationService = new RecordingTradingFlowNotificationService();
        paperPortfolioService = paperPortfolioService();
        positionExitProperties = new PositionExitProperties();
        safetyProperties = new SafetyProperties();
        tradingFlowService = new TradingFlowService(
                marketPriceProvider,
                new SimpleThresholdStrategy(strategyProperties),
                new OrderRequestFactory(),
                new OrderExecutionService(
                        new PaperTradingExecutionGateway(),
                        new RiskValidationService(tradingProperties),
                        dailyRiskValidationService(paperPortfolioService),
                        paperPortfolioService
                ),
                new TradingFlowHistoryService(historyRepository),
                notificationProperties,
                new NotificationPolicyService(notificationProperties),
                notificationService,
                new PositionExitSignalService(positionExitProperties, paperPortfolioService),
                new KillSwitchService(safetyProperties)
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
    void runKeepsExistingFlowWhenKillSwitchIsDisabled() {
        safetyProperties.setKillSwitchEnabled(false);
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("100"));

        TradingFlowResult result = tradingFlowService.run("KRW-BTC");

        assertThat(result.orderStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void runBlocksBeforeTradingFlowWhenKillSwitchIsEnabled() {
        safetyProperties.setKillSwitchEnabled(true);

        TradingFlowResult result = tradingFlowService.run("KRW-BTC");

        assertThat(result.orderStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(result.orderCreated()).isFalse();
        assertThat(result.message()).isEqualTo("Kill switch enabled: trading flow blocked");
        assertThat(historyRepository.findRecent(1).getFirst().message()).isEqualTo("Kill switch enabled: trading flow blocked");
        assertThat(paperPortfolioService.getPortfolio().cash()).isEqualByComparingTo("1000000");
    }

    @Test
    void runFillsPaperSellOrderWhenPriceIsAtOrAboveSellThreshold() {
        paperPortfolioService.apply(new com.giseop.comebot.execution.domain.OrderResult(
                "KRW-BTC",
                com.giseop.comebot.execution.domain.OrderSide.BUY,
                new BigDecimal("1"),
                new BigDecimal("100"),
                OrderStatus.FILLED,
                "seed",
                java.time.Instant.now()
        ));
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
        assertThat(historyRepository.findRecent(1)).isEmpty();
        assertThat(paperPortfolioService.getPortfolio().cash()).isEqualByComparingTo("1000000");
        assertThat(paperPortfolioService.findPositions()).isEmpty();
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
        assertThat(paperPortfolioService.getPortfolio().cash()).isEqualByComparingTo("1000000");
        assertThat(paperPortfolioService.findPositions()).isEmpty();
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
        assertThat(historyRepository.findRecent(1)).isEmpty();
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

    @Test
    void runKeepsStrategyFlowWhenPositionExitIsDisabled() {
        paperPortfolioService.apply(new com.giseop.comebot.execution.domain.OrderResult(
                "KRW-BTC",
                com.giseop.comebot.execution.domain.OrderSide.BUY,
                new BigDecimal("1"),
                new BigDecimal("100"),
                OrderStatus.FILLED,
                "seed",
                java.time.Instant.now()
        ));
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("105"));

        TradingFlowResult result = tradingFlowService.run("KRW-BTC");

        assertThat(result.signalType()).isEqualTo(SignalType.HOLD);
        assertThat(result.orderCreated()).isFalse();
    }

    @Test
    void runCreatesSellSignalWhenTakeProfitRateIsReached() {
        positionExitProperties.setPositionExitEnabled(true);
        paperPortfolioService.apply(new com.giseop.comebot.execution.domain.OrderResult(
                "KRW-BTC",
                com.giseop.comebot.execution.domain.OrderSide.BUY,
                new BigDecimal("1"),
                new BigDecimal("100"),
                OrderStatus.FILLED,
                "seed",
                java.time.Instant.now()
        ));
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("105"));

        TradingFlowResult result = tradingFlowService.run("KRW-BTC");

        assertThat(result.signalType()).isEqualTo(SignalType.SELL);
        assertThat(result.signalReason()).contains("Take profit rate reached");
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void runCreatesSellSignalWhenStopLossRateIsReached() {
        StrategyProperties strategyProperties = new StrategyProperties();
        strategyProperties.setBuyPrice(new BigDecimal("1"));
        strategyProperties.setSellPrice(new BigDecimal("200"));
        strategyProperties.setOrderQuantity(new BigDecimal("1"));
        positionExitProperties.setPositionExitEnabled(true);
        paperPortfolioService.apply(new com.giseop.comebot.execution.domain.OrderResult(
                "KRW-BTC",
                com.giseop.comebot.execution.domain.OrderSide.BUY,
                new BigDecimal("1"),
                new BigDecimal("100"),
                OrderStatus.FILLED,
                "seed",
                java.time.Instant.now()
        ));
        TradingFlowService service = tradingFlowService(strategyProperties, positionExitProperties, paperPortfolioService);
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("97"));

        TradingFlowResult result = service.run("KRW-BTC");

        assertThat(result.signalType()).isEqualTo(SignalType.SELL);
        assertThat(result.signalReason()).contains("Stop loss rate reached");
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void runDoesNotCreateExitSellWhenPositionIsEmpty() {
        positionExitProperties.setPositionExitEnabled(true);
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("105"));

        TradingFlowResult result = tradingFlowService.run("KRW-BTC");

        assertThat(result.signalType()).isEqualTo(SignalType.HOLD);
        assertThat(result.orderCreated()).isFalse();
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

    private PaperPortfolioService paperPortfolioService() {
        PaperPortfolioProperties properties = new PaperPortfolioProperties();
        properties.setInitialCash(new BigDecimal("1000000"));
        InMemoryPaperPortfolioRepository repository = new InMemoryPaperPortfolioRepository();
        PaperPortfolioService service = new PaperPortfolioService(repository, properties);
        service.initialize();
        return service;
    }

    private TradingFlowService tradingFlowService(
            StrategyProperties strategyProperties,
            PositionExitProperties positionExitProperties,
            PaperPortfolioService paperPortfolioService
    ) {
        TradingProperties tradingProperties = new TradingProperties();
        tradingProperties.setMaxOrderAmount(new BigDecimal("100000"));
        tradingProperties.setAllowedMarkets(List.of("KRW-BTC", "KRW-ETH"));

        return new TradingFlowService(
                marketPriceProvider,
                new SimpleThresholdStrategy(strategyProperties),
                new OrderRequestFactory(),
                new OrderExecutionService(
                        new PaperTradingExecutionGateway(),
                        new RiskValidationService(tradingProperties),
                        dailyRiskValidationService(paperPortfolioService),
                        paperPortfolioService
                ),
                new TradingFlowHistoryService(historyRepository),
                notificationProperties,
                new NotificationPolicyService(notificationProperties),
                notificationService,
                new PositionExitSignalService(positionExitProperties, paperPortfolioService),
                new KillSwitchService(safetyProperties)
        );
    }

    private DailyRiskValidationService dailyRiskValidationService(PaperPortfolioService paperPortfolioService) {
        return new DailyRiskValidationService(
                new DailyRiskProperties(),
                new TradingFlowHistoryService(historyRepository),
                paperPortfolioService
        );
    }
}
