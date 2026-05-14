package com.giseop.comebot.trading.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.gateway.PaperTradingExecutionGateway;
import com.giseop.comebot.execution.service.OrderExecutionService;
import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.history.repository.InMemoryTradingFlowHistoryRepository;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.market.provider.MarketPriceProvider;
import com.giseop.comebot.notification.NotificationPolicyService;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.notification.TradingFlowNotificationService;
import com.giseop.comebot.portfolio.PaperPortfolioProperties;
import com.giseop.comebot.portfolio.domain.PaperTradeLog;
import com.giseop.comebot.portfolio.repository.InMemoryPaperPortfolioRepository;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.risk.DailyRiskProperties;
import com.giseop.comebot.risk.PositionExitProperties;
import com.giseop.comebot.risk.service.DailyRiskValidationService;
import com.giseop.comebot.risk.service.PositionExitSignalService;
import com.giseop.comebot.risk.service.RiskValidationService;
import com.giseop.comebot.safety.KillSwitchService;
import com.giseop.comebot.safety.SafetyProperties;
import com.giseop.comebot.scheduler.PositionExitRunSummary;
import com.giseop.comebot.scheduler.PositionExitSchedulerProperties;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.strategy.service.OrderRequestFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaperPositionCloseSmokeTest {

    private RecordingPaperPortfolioRepository portfolioRepository;
    private PaperPortfolioService portfolioService;
    private InMemoryTradingFlowHistoryRepository historyRepository;
    private StubMarketPriceProvider marketPriceProvider;
    private PositionExitProperties positionExitProperties;
    private PositionExitExecutionService positionExitExecutionService;

    @BeforeEach
    void setUp() {
        portfolioRepository = new RecordingPaperPortfolioRepository();
        PaperPortfolioProperties portfolioProperties = new PaperPortfolioProperties();
        portfolioProperties.setInitialCash(new BigDecimal("1000000"));
        portfolioService = new PaperPortfolioService(portfolioRepository, portfolioProperties);
        portfolioService.initialize();

        historyRepository = new InMemoryTradingFlowHistoryRepository();
        TradingFlowHistoryService historyService = new TradingFlowHistoryService(historyRepository);
        marketPriceProvider = new StubMarketPriceProvider();
        positionExitProperties = new PositionExitProperties();
        positionExitProperties.setPositionExitEnabled(true);
        positionExitProperties.setTakeProfitRate(new BigDecimal("5"));
        positionExitProperties.setStopLossRate(new BigDecimal("-3"));

        TradingProperties tradingProperties = new TradingProperties();
        tradingProperties.setAllowedMarkets(List.of("KRW-BTC"));
        tradingProperties.setMaxOrderAmount(new BigDecimal("10000000"));

        OrderExecutionService orderExecutionService = new OrderExecutionService(
                new PaperTradingExecutionGateway(),
                new RiskValidationService(tradingProperties),
                new DailyRiskValidationService(new DailyRiskProperties(), historyService, portfolioService),
                portfolioService
        );

        positionExitExecutionService = new PositionExitExecutionService(
                portfolioService,
                marketPriceProvider,
                new PositionExitSignalService(positionExitProperties, portfolioService),
                new OrderRequestFactory(),
                orderExecutionService,
                historyService,
                new PositionExitSchedulerProperties(),
                new NotificationProperties(),
                mock(NotificationPolicyService.class),
                mock(TradingFlowNotificationService.class),
                new KillSwitchService(new SafetyProperties())
        );
    }

    @Test
    void takeProfitExitSellsPaperPositionAndWritesHistoryAndTradeLog() {
        portfolioService.apply(ExchangeMode.UPBIT, filled("KRW-BTC", OrderSide.BUY, "2", "100"));
        marketPriceProvider.prices = Map.of("KRW-BTC", new BigDecimal("106"));

        PositionExitRunSummary summary = positionExitExecutionService.execute(ExchangeMode.UPBIT);

        assertThat(summary.soldCount()).isEqualTo(1);
        assertThat(summary.failedCount()).isZero();
        assertThat(portfolioService.findPositions(ExchangeMode.UPBIT)).isEmpty();
        assertThat(portfolioService.getPortfolio(ExchangeMode.UPBIT).cash()).isEqualByComparingTo("1000012");
        assertThat(portfolioService.getPortfolio(ExchangeMode.UPBIT).realizedProfit()).isEqualByComparingTo("12");

        TradingFlowHistory history = historyRepository.findRecent(ExchangeMode.UPBIT, 10).getFirst();
        assertThat(history.signalType()).isEqualTo(SignalType.SELL);
        assertThat(history.orderStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(history.signalReason()).startsWith("Take profit rate reached");

        assertThat(portfolioRepository.tradeLogs()).hasSize(2);
        assertThat(portfolioRepository.tradeLogs().getFirst().side()).isEqualTo(OrderSide.SELL);
        assertThat(portfolioRepository.tradeLogs().getFirst().realizedProfit()).isEqualByComparingTo("12");
        assertThat(portfolioRepository.tradeLogs().get(1).side()).isEqualTo(OrderSide.BUY);
    }

    @Test
    void stopLossExitSellsPaperPositionAndRecordsRealizedLoss() {
        portfolioService.apply(ExchangeMode.UPBIT, filled("KRW-BTC", OrderSide.BUY, "2", "100"));
        marketPriceProvider.prices = Map.of("KRW-BTC", new BigDecimal("96"));

        PositionExitRunSummary summary = positionExitExecutionService.execute(ExchangeMode.UPBIT);

        assertThat(summary.soldCount()).isEqualTo(1);
        assertThat(portfolioService.findPositions(ExchangeMode.UPBIT)).isEmpty();
        assertThat(portfolioService.getPortfolio(ExchangeMode.UPBIT).cash()).isEqualByComparingTo("999992");
        assertThat(portfolioService.getPortfolio(ExchangeMode.UPBIT).realizedProfit()).isEqualByComparingTo("-8");
        assertThat(historyRepository.findRecent(ExchangeMode.UPBIT, 10).getFirst().signalReason())
                .startsWith("Stop loss rate reached");
        assertThat(portfolioRepository.tradeLogs().getFirst().realizedProfit()).isEqualByComparingTo("-8");
    }

    @Test
    void missingExitPriceFailsWithoutChangingPaperPositionOrTradeLog() {
        portfolioService.apply(ExchangeMode.UPBIT, filled("KRW-BTC", OrderSide.BUY, "2", "100"));
        int tradeLogCountBefore = portfolioRepository.tradeLogs().size();

        PositionExitRunSummary summary = positionExitExecutionService.execute(ExchangeMode.UPBIT);

        assertThat(summary.failedCount()).isEqualTo(1);
        assertThat(portfolioService.findPositions(ExchangeMode.UPBIT)).hasSize(1);
        assertThat(portfolioService.getPortfolio(ExchangeMode.UPBIT).cash()).isEqualByComparingTo("999800");
        assertThat(portfolioRepository.tradeLogs()).hasSize(tradeLogCountBefore);
        assertThat(historyRepository.findRecent(ExchangeMode.UPBIT, 10).getFirst().orderStatus())
                .isEqualTo(OrderStatus.FAILED);
    }

    private OrderResult filled(String market, OrderSide side, String quantity, String price) {
        return new OrderResult(
                market,
                side,
                new BigDecimal(quantity),
                new BigDecimal(price),
                OrderStatus.FILLED,
                "filled",
                Instant.now()
        );
    }

    private static final class StubMarketPriceProvider implements MarketPriceProvider {

        private Map<String, BigDecimal> prices = Map.of();

        @Override
        public MarketPrice getCurrentPrice(String market) {
            BigDecimal price = prices.get(market);
            return price == null ? null : new MarketPrice(market, price, Instant.now());
        }
    }

    private static final class RecordingPaperPortfolioRepository extends InMemoryPaperPortfolioRepository {

        private final List<PaperTradeLog> tradeLogs = new ArrayList<>();

        @Override
        public void saveTradeLog(ExchangeMode exchange, PaperTradeLog tradeLog) {
            super.saveTradeLog(exchange, tradeLog);
            tradeLogs.addFirst(tradeLog);
        }

        List<PaperTradeLog> tradeLogs() {
            return tradeLogs;
        }
    }
}
