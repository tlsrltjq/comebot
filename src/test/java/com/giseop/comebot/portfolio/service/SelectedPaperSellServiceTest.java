package com.giseop.comebot.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.gateway.PaperTradingExecutionGateway;
import com.giseop.comebot.execution.service.OrderExecutionService;
import com.giseop.comebot.history.repository.InMemoryTradingFlowHistoryRepository;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.market.provider.MarketPriceProvider;
import com.giseop.comebot.portfolio.PaperPortfolioProperties;
import com.giseop.comebot.portfolio.domain.PaperTradeLog;
import com.giseop.comebot.portfolio.dto.SelectedPaperSellResponse;
import com.giseop.comebot.portfolio.repository.InMemoryPaperPortfolioRepository;
import com.giseop.comebot.risk.DailyRiskProperties;
import com.giseop.comebot.risk.service.DailyRiskValidationService;
import com.giseop.comebot.risk.service.RiskValidationService;
import com.giseop.comebot.safety.KillSwitchService;
import com.giseop.comebot.safety.SafetyProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SelectedPaperSellServiceTest {

    private RecordingPaperPortfolioRepository portfolioRepository;
    private PaperPortfolioService portfolioService;
    private InMemoryTradingFlowHistoryRepository historyRepository;
    private TradingFlowHistoryService historyService;
    private StubMarketPriceProvider marketPriceProvider;
    private SafetyProperties safetyProperties;
    private SelectedPaperSellService service;

    @BeforeEach
    void setUp() {
        PaperPortfolioProperties portfolioProperties = new PaperPortfolioProperties();
        portfolioProperties.setInitialCash(new BigDecimal("1000000"));
        portfolioRepository = new RecordingPaperPortfolioRepository();
        portfolioService = new PaperPortfolioService(portfolioRepository, portfolioProperties);
        portfolioService.initialize();

        historyRepository = new InMemoryTradingFlowHistoryRepository();
        historyService = new TradingFlowHistoryService(historyRepository);
        marketPriceProvider = new StubMarketPriceProvider();
        safetyProperties = new SafetyProperties();

        TradingProperties tradingProperties = new TradingProperties();
        tradingProperties.setAllowedMarkets(List.of("KRW-BTC", "KRW-ETH"));
        tradingProperties.setMaxOrderAmount(new BigDecimal("2000000"));

        OrderExecutionService orderExecutionService = new OrderExecutionService(
                new PaperTradingExecutionGateway(),
                new RiskValidationService(tradingProperties),
                new DailyRiskValidationService(new DailyRiskProperties(), historyService, portfolioService),
                portfolioService
        );
        service = new SelectedPaperSellService(
                portfolioService,
                marketPriceProvider,
                orderExecutionService,
                historyService,
                new KillSwitchService(safetyProperties)
        );
    }

    @Test
    void selectedHeldPositionIsSoldAndHistoryIsSaved() {
        portfolioService.apply(new OrderResult("KRW-BTC", OrderSide.BUY, new BigDecimal("2"), new BigDecimal("100"),
                OrderStatus.FILLED, "filled", Instant.now()));
        marketPriceProvider.prices = Map.of("KRW-BTC", new BigDecimal("120"));

        SelectedPaperSellResponse response = service.sellSelected(ExchangeMode.UPBIT, List.of("KRW-BTC"));

        assertThat(response.succeededCount()).isEqualTo(1);
        assertThat(response.failedCount()).isZero();
        assertThat(portfolioService.findPositions()).isEmpty();
        assertThat(portfolioService.getPortfolio().cash()).isEqualByComparingTo("1000040");
        assertThat(portfolioService.getPortfolio().realizedProfit()).isEqualByComparingTo("40");
        assertThat(historyRepository.findRecent(ExchangeMode.UPBIT, 10).getFirst().orderStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(portfolioRepository.tradeLogs()).hasSize(2);
        assertThat(portfolioRepository.tradeLogs().getFirst().side()).isEqualTo(OrderSide.SELL);
        assertThat(portfolioRepository.tradeLogs().getFirst().realizedProfit()).isEqualByComparingTo("40");
    }

    @Test
    void selectedSellCanCloseHeldPositionOutsideAllowedEntryMarkets() {
        portfolioService.apply(new OrderResult("KRW-XRP", OrderSide.BUY, new BigDecimal("3"), new BigDecimal("100"),
                OrderStatus.FILLED, "filled", Instant.now()));
        marketPriceProvider.prices = Map.of("KRW-XRP", new BigDecimal("90"));

        SelectedPaperSellResponse response = service.sellSelected(ExchangeMode.UPBIT, List.of("KRW-XRP"));

        assertThat(response.succeededCount()).isEqualTo(1);
        assertThat(response.failedCount()).isZero();
        assertThat(portfolioService.findPosition(ExchangeMode.UPBIT, "KRW-XRP").orElseThrow().quantity())
                .isEqualByComparingTo("0");
        assertThat(historyRepository.findRecent(ExchangeMode.UPBIT, 10).getFirst().orderStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void duplicateMarketsAreProcessedOnceAndMissingPositionIsRejected() {
        portfolioService.apply(new OrderResult("KRW-BTC", OrderSide.BUY, BigDecimal.ONE, new BigDecimal("100"),
                OrderStatus.FILLED, "filled", Instant.now()));
        marketPriceProvider.prices = Map.of("KRW-BTC", new BigDecimal("110"));

        SelectedPaperSellResponse response = service.sellSelected(ExchangeMode.UPBIT, List.of("KRW-BTC", "KRW-BTC", "KRW-ETH"));

        assertThat(response.requestedCount()).isEqualTo(2);
        assertThat(response.succeededCount()).isEqualTo(1);
        assertThat(response.failedCount()).isEqualTo(1);
        assertThat(response.results()).extracting("market").containsExactly("KRW-BTC", "KRW-ETH");
        assertThat(response.results().get(1).orderStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(historyRepository.findRecent(ExchangeMode.UPBIT, 10)).hasSize(2);
    }

    @Test
    void killSwitchRejectsBeforePriceLookup() {
        portfolioService.apply(new OrderResult("KRW-BTC", OrderSide.BUY, BigDecimal.ONE, new BigDecimal("100"),
                OrderStatus.FILLED, "filled", Instant.now()));
        safetyProperties.setKillSwitchEnabled(true);

        SelectedPaperSellResponse response = service.sellSelected(ExchangeMode.UPBIT, List.of("KRW-BTC"));

        assertThat(response.succeededCount()).isZero();
        assertThat(response.results().getFirst().orderStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(marketPriceProvider.singleRequests).isEmpty();
        assertThat(portfolioService.findPositions()).hasSize(1);
    }

    @Test
    void binanceSellUsesBinancePortfolioOnly() {
        portfolioService.apply(ExchangeMode.BINANCE, new OrderResult("BTCUSDT", OrderSide.BUY, new BigDecimal("0.01"), new BigDecimal("50000"),
                OrderStatus.FILLED, "filled", Instant.now()));
        marketPriceProvider.prices = Map.of("BTCUSDT", new BigDecimal("55000"));

        SelectedPaperSellResponse response = service.sellSelected(ExchangeMode.BINANCE, List.of("BTCUSDT"));

        assertThat(response.exchange()).isEqualTo("BINANCE");
        assertThat(response.succeededCount()).isEqualTo(1);
        assertThat(portfolioService.getPortfolio(ExchangeMode.UPBIT).cash()).isEqualByComparingTo("1000000");
        assertThat(portfolioService.getPortfolio(ExchangeMode.BINANCE).cash()).isEqualByComparingTo("1050.00");
        assertThat(historyRepository.findRecent(ExchangeMode.BINANCE, 10)).hasSize(1);
        assertThat(historyRepository.findRecent(ExchangeMode.UPBIT, 10)).isEmpty();
    }

    @Test
    void priceLookupFailureDoesNotChangePortfolio() {
        portfolioService.apply(new OrderResult("KRW-BTC", OrderSide.BUY, BigDecimal.ONE, new BigDecimal("100"),
                OrderStatus.FILLED, "filled", Instant.now()));
        marketPriceProvider.fail = true;

        SelectedPaperSellResponse response = service.sellSelected(ExchangeMode.UPBIT, List.of("KRW-BTC"));

        assertThat(response.succeededCount()).isZero();
        assertThat(response.results().getFirst().orderStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(portfolioService.findPositions()).hasSize(1);
        assertThat(portfolioRepository.tradeLogs()).hasSize(1);
        assertThat(portfolioRepository.tradeLogs().getFirst().side()).isEqualTo(OrderSide.BUY);
        assertThat(historyRepository.findRecent(ExchangeMode.UPBIT, 10).getFirst().orderStatus()).isEqualTo(OrderStatus.FAILED);
    }

    private static class StubMarketPriceProvider implements MarketPriceProvider {
        private Map<String, BigDecimal> prices = Map.of();
        private boolean fail;
        private final java.util.ArrayList<String> singleRequests = new java.util.ArrayList<>();

        @Override
        public MarketPrice getCurrentPrice(String market) {
            singleRequests.add(market);
            if (fail) {
                throw new IllegalStateException("Current price lookup failed");
            }
            return new MarketPrice(market, prices.get(market), Instant.now());
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
