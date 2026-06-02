package com.giseop.comebot.strategy.candidate;

import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.candle.domain.Candle;
import com.giseop.comebot.market.candle.provider.BinanceCandleProvider;
import com.giseop.comebot.market.candle.provider.CandleProvider;
import com.giseop.comebot.market.candle.provider.UpbitCandleProvider;
import com.giseop.comebot.market.service.BtcTrendCacheService;
import com.giseop.comebot.market.service.MarketSelectionService;
import com.giseop.comebot.strategy.indicator.VolatilityIndicatorService;
import com.giseop.comebot.strategy.indicator.VolatilitySnapshot;
import com.giseop.comebot.strategy.service.StrategyMarketSettingsService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CandidateScannerService {

    private static final Logger log = LoggerFactory.getLogger(CandidateScannerService.class);

    private final TradingProperties tradingProperties;
    private final CandidateScannerProperties candidateScannerProperties;
    private final CandleProvider upbitCandleProvider;
    private final CandleProvider binanceCandleProvider;
    private final VolatilityIndicatorService volatilityIndicatorService;
    private final StrategyMarketSettingsService strategyMarketSettingsService;
    private final MarketSelectionService marketSelectionService;
    private final BtcTrendCacheService btcTrendCacheService;

    @Autowired
    public CandidateScannerService(
            TradingProperties tradingProperties,
            CandidateScannerProperties candidateScannerProperties,
            UpbitCandleProvider upbitCandleProvider,
            VolatilityIndicatorService volatilityIndicatorService,
            StrategyMarketSettingsService strategyMarketSettingsService,
            MarketSelectionService marketSelectionService,
            BtcTrendCacheService btcTrendCacheService
    ) {
        this.tradingProperties = tradingProperties;
        this.candidateScannerProperties = candidateScannerProperties;
        this.upbitCandleProvider = upbitCandleProvider;
        this.binanceCandleProvider = new BinanceCandleProvider();
        this.volatilityIndicatorService = volatilityIndicatorService;
        this.strategyMarketSettingsService = strategyMarketSettingsService;
        this.marketSelectionService = marketSelectionService;
        this.btcTrendCacheService = btcTrendCacheService;
    }

    CandidateScannerService(
            TradingProperties tradingProperties,
            CandidateScannerProperties candidateScannerProperties,
            CandleProvider candleProvider,
            VolatilityIndicatorService volatilityIndicatorService,
            StrategyMarketSettingsService strategyMarketSettingsService
    ) {
        this(
                tradingProperties,
                candidateScannerProperties,
                candleProvider,
                candleProvider,
                volatilityIndicatorService,
                strategyMarketSettingsService,
                new MarketSelectionService(new com.giseop.comebot.market.service.UpbitKrwTickerStore()),
                null
        );
    }

    CandidateScannerService(
            TradingProperties tradingProperties,
            CandidateScannerProperties candidateScannerProperties,
            CandleProvider upbitCandleProvider,
            CandleProvider binanceCandleProvider,
            VolatilityIndicatorService volatilityIndicatorService,
            StrategyMarketSettingsService strategyMarketSettingsService,
            MarketSelectionService marketSelectionService,
            BtcTrendCacheService btcTrendCacheService
    ) {
        this.tradingProperties = tradingProperties;
        this.candidateScannerProperties = candidateScannerProperties;
        this.upbitCandleProvider = upbitCandleProvider;
        this.binanceCandleProvider = binanceCandleProvider;
        this.volatilityIndicatorService = volatilityIndicatorService;
        this.strategyMarketSettingsService = strategyMarketSettingsService;
        this.marketSelectionService = marketSelectionService;
        this.btcTrendCacheService = btcTrendCacheService;
    }

    public List<TradingCandidate> scanAllowedMarkets() {
        return scanAllowedMarkets(ExchangeMode.UPBIT);
    }

    public List<TradingCandidate> scanAllowedMarkets(ExchangeMode exchange) {
        return scanAllowedMarkets(exchange, Integer.MAX_VALUE);
    }

    public List<TradingCandidate> scanAllowedMarkets(ExchangeMode exchange, int limit) {
        List<String> configuredMarkets = exchange == ExchangeMode.BINANCE
                ? binanceCandidateMarkets()
                : tradingProperties.getAllowedMarkets();
        List<String> markets = marketSelectionService.resolve(exchange, configuredMarkets);
        return scanMarkets(exchange, limitMarkets(markets, limit));
    }

    private List<String> limitMarkets(List<String> markets, int limit) {
        if (limit <= 0 || markets.size() <= limit) {
            return markets;
        }
        return markets.subList(0, limit);
    }

    private List<TradingCandidate> scanMarkets(ExchangeMode exchange, List<String> markets) {
        if (markets.size() <= 1) {
            return markets.stream()
                    .map(market -> scan(exchange, market))
                    .toList();
        }

        int workerCount = Math.min(6, markets.size());
        List<Callable<TradingCandidate>> tasks = markets.stream()
                .<Callable<TradingCandidate>>map(market -> () -> scan(exchange, market))
                .toList();
        try (ExecutorService executorService = Executors.newFixedThreadPool(workerCount)) {
            return executorService.invokeAll(tasks).stream()
                    .map(future -> candidateFromFuture(exchange, future))
                    .toList();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Candidate scan interrupted. exchange={}", exchange);
            return markets.stream()
                    .map(market -> skippedCandidate(market, "Candidate scan interrupted"))
                    .toList();
        }
    }

    private TradingCandidate candidateFromFuture(
            ExchangeMode exchange,
            java.util.concurrent.Future<TradingCandidate> future
    ) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Candidate scan future interrupted. exchange={}", exchange);
            return skippedCandidate(null, "Candidate scan interrupted");
        } catch (ExecutionException exception) {
            log.warn("Candidate scan future failed. exchange={}, error={}", exchange, exception.getClass().getSimpleName());
            return skippedCandidate(null, "Candidate scan failed: " + failureReason(exception));
        }
    }

    private List<String> binanceCandidateMarkets() {
        List<String> configuredMarkets = tradingProperties.getAllowedMarkets();
        boolean hasBinanceMarket = configuredMarkets.stream()
                .anyMatch(market -> MarketSelectionService.ALL_USDT.equalsIgnoreCase(market)
                        || (market != null && market.trim().toUpperCase().endsWith("USDT")));
        if (hasBinanceMarket) {
            return configuredMarkets;
        }
        return List.of(MarketSelectionService.ALL_USDT);
    }

    public TradingCandidate scan(String market) {
        return scan(ExchangeMode.UPBIT, market);
    }

    public TradingCandidate scan(ExchangeMode exchange, String market) {
        try {
            List<Candle> candles = candleProvider(exchange).getRecentCandles(
                    market,
                    candidateScannerProperties.getCandleUnitMinutes(exchange),
                    candidateScannerProperties.getCandleCount(exchange)
            );
            List<Candle> completedCandles = removeIncompleteLatestCandle(
                    candles,
                    candidateScannerProperties.getCandleUnitMinutes(exchange)
            );
            List<Candle> validCandles = completedCandles.stream()
                    .filter(this::hasPositiveTradeAmount)
                    .toList();
            if (validCandles.size() < 2) {
                return new TradingCandidate(
                        market,
                        CandidateDecision.SKIPPED,
                        "Not enough valid trade amount candles",
                        null, null, null, null, null, null,
                        Instant.now()
                );
            }
            VolatilitySnapshot snapshot = volatilityIndicatorService.calculate(validCandles);
            BigDecimal minTradeAmount = minLatestCandleTradeAmount(exchange);
            if (minTradeAmount.compareTo(BigDecimal.ZERO) > 0
                    && snapshot.latestCandleTradeAmount().compareTo(minTradeAmount) < 0) {
                return skipped(snapshot, "Latest candle trade amount is below minimum threshold");
            }
            return toCandidate(snapshot);
        } catch (RuntimeException exception) {
            log.warn("Candidate scan failed. market={}, reason={}", market, failureReason(exception));
            return new TradingCandidate(
                    market,
                    CandidateDecision.SKIPPED,
                    "Candidate scan failed: " + failureReason(exception),
                    null, null, null, null, null, null,
                    Instant.now()
            );
        }
    }

    private List<Candle> removeIncompleteLatestCandle(List<Candle> candles, int unitMinutes) {
        if (candles == null || candles.size() <= 1) {
            return candles == null ? List.of() : candles;
        }
        List<Candle> orderedCandles = candles.stream()
                .filter(candle -> candle != null && candle.candleTime() != null)
                .sorted(java.util.Comparator.comparing(Candle::candleTime))
                .toList();
        if (orderedCandles.size() <= 1) {
            return orderedCandles;
        }
        Candle latest = orderedCandles.getLast();
        Instant latestCloseTime = latest.candleTime().plus(Duration.ofMinutes(unitMinutes));
        if (Instant.now().isBefore(latestCloseTime)) {
            return orderedCandles.subList(0, orderedCandles.size() - 1);
        }
        return orderedCandles;
    }

    private TradingCandidate skippedCandidate(String market, String reason) {
        return new TradingCandidate(
                market,
                CandidateDecision.SKIPPED,
                reason,
                null, null, null, null, null, null,
                Instant.now()
        );
    }

    private CandleProvider candleProvider(ExchangeMode exchange) {
        return exchange == ExchangeMode.BINANCE ? binanceCandleProvider : upbitCandleProvider;
    }

    private BigDecimal minLatestCandleTradeAmount(ExchangeMode exchange) {
        if (exchange == ExchangeMode.BINANCE) {
            return candidateScannerProperties.getMinLatestCandleTradeAmountUsdt(exchange);
        }
        return candidateScannerProperties.getMinLatestCandleTradeAmountKrw(exchange);
    }

    private boolean hasPositiveTradeAmount(Candle candle) {
        return candle != null
                && candle.accumulatedTradePrice() != null
                && candle.accumulatedTradePrice().compareTo(BigDecimal.ZERO) > 0;
    }

    private String failureReason(Throwable exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        Throwable rootCause = rootCause(exception);
        if (rootCause == exception || rootCause.getMessage() == null || rootCause.getMessage().isBlank()) {
            return exception.getClass().getSimpleName() + " - " + exception.getMessage();
        }
        return exception.getClass().getSimpleName()
                + " - " + exception.getMessage()
                + " (cause: " + rootCause.getClass().getSimpleName() + " - " + rootCause.getMessage() + ")";
    }

    private Throwable rootCause(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private TradingCandidate toCandidate(VolatilitySnapshot snapshot) {
        // Weak downtrend filter: net move from start to end must not be negative
        if (snapshot.priceChangeRate().compareTo(BigDecimal.ZERO) < 0) {
            return skipped(snapshot, "Trend is not UP");
        }
        // Pump must have occurred somewhere in the window
        if (snapshot.windowHighChangeRate().compareTo(strategyMarketSettingsService.minPriceChangeRate(snapshot.market())) < 0) {
            return skipped(snapshot, "No significant pump detected in window");
        }
        if (snapshot.windowHighChangeRate().compareTo(strategyMarketSettingsService.maxPriceChangeRate(snapshot.market())) > 0) {
            return skipped(snapshot, "Price change rate is overheated");
        }
        // Volume spike must have occurred at peak (not just latest candle)
        if (snapshot.peakTradeAmountChangeRate().compareTo(strategyMarketSettingsService.minTradeAmountChangeRate(snapshot.market())) < 0) {
            return skipped(snapshot, "Trade amount change rate is below threshold");
        }
        if (snapshot.highLowRangeRate().compareTo(strategyMarketSettingsService.maxHighLowRangeRate(snapshot.market())) > 0) {
            return skipped(snapshot, "High low range rate is overheated");
        }
        // Pullback zone: price must have pulled back from the high (not at the peak)
        ExchangeMode exchange = exchangeOf(snapshot.market());
        BigDecimal minDistFromHigh = candidateScannerProperties.getMinDistanceFromHighRate(exchange);
        if (minDistFromHigh.compareTo(BigDecimal.ZERO) > 0
                && snapshot.distanceFromHighRate().compareTo(minDistFromHigh) < 0) {
            return skipped(snapshot, "Price has not pulled back sufficiently from high");
        }
        BigDecimal maxDistFromHigh = candidateScannerProperties.getMaxDistanceFromHighRate(exchange);
        if (maxDistFromHigh.compareTo(BigDecimal.ZERO) > 0
                && snapshot.distanceFromHighRate().compareTo(maxDistFromHigh) > 0) {
            return skipped(snapshot, "Price is too far below the recent high");
        }
        // Bounce confirmation: last candle must be bullish
        if (!snapshot.lastCandleBullish()) {
            return skipped(snapshot, "Last candle is not bullish");
        }
        // BTC 1h trend filter (UPBIT markets only)
        if (btcTrendCacheService != null && snapshot.market() != null && !snapshot.market().endsWith("USDT")) {
            BtcTrendCacheService.BtcTrend btcTrend = btcTrendCacheService.trend();
            if (btcTrend == BtcTrendCacheService.BtcTrend.DOWN) {
                return skipped(snapshot, "BTC 1h trend is DOWN");
            }
        }
        return new TradingCandidate(
                snapshot.market(),
                CandidateDecision.SELECTED,
                "Pullback bounce candidate selected",
                snapshot.currentPrice(),
                snapshot.priceChangeRate(),
                snapshot.highLowRangeRate(),
                snapshot.tradeAmountChangeRate(),
                snapshot.trend(),
                snapshot.lastCandleBullish(),
                Instant.now()
        );
    }

    private ExchangeMode exchangeOf(String market) {
        if (market != null && market.endsWith("USDT") && !market.startsWith("KRW-")) {
            return ExchangeMode.BINANCE;
        }
        return ExchangeMode.UPBIT;
    }

    private TradingCandidate skipped(VolatilitySnapshot snapshot, String reason) {
        return new TradingCandidate(
                snapshot.market(),
                CandidateDecision.SKIPPED,
                reason,
                snapshot.currentPrice(),
                snapshot.priceChangeRate(),
                snapshot.highLowRangeRate(),
                snapshot.tradeAmountChangeRate(),
                snapshot.trend(),
                snapshot.lastCandleBullish(),
                Instant.now()
        );
    }
}
