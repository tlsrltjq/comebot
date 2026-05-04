package com.giseop.comebot.strategy.candidate;

import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.market.candle.domain.Candle;
import com.giseop.comebot.market.candle.provider.CandleProvider;
import com.giseop.comebot.market.service.MarketSelectionService;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import com.giseop.comebot.strategy.indicator.VolatilityIndicatorService;
import com.giseop.comebot.strategy.indicator.VolatilitySnapshot;
import com.giseop.comebot.strategy.service.StrategyMarketSettingsService;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CandidateScannerService {

    private static final Logger log = LoggerFactory.getLogger(CandidateScannerService.class);

    private final TradingProperties tradingProperties;
    private final CandidateScannerProperties candidateScannerProperties;
    private final CandleProvider candleProvider;
    private final VolatilityIndicatorService volatilityIndicatorService;
    private final StrategyMarketSettingsService strategyMarketSettingsService;
    private final MarketSelectionService marketSelectionService;

    @Autowired
    public CandidateScannerService(
            TradingProperties tradingProperties,
            CandidateScannerProperties candidateScannerProperties,
            CandleProvider candleProvider,
            VolatilityIndicatorService volatilityIndicatorService,
            StrategyMarketSettingsService strategyMarketSettingsService,
            MarketSelectionService marketSelectionService
    ) {
        this.tradingProperties = tradingProperties;
        this.candidateScannerProperties = candidateScannerProperties;
        this.candleProvider = candleProvider;
        this.volatilityIndicatorService = volatilityIndicatorService;
        this.strategyMarketSettingsService = strategyMarketSettingsService;
        this.marketSelectionService = marketSelectionService;
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
                volatilityIndicatorService,
                strategyMarketSettingsService,
                new MarketSelectionService(new com.giseop.comebot.market.service.UpbitKrwTickerStore())
        );
    }

    public List<TradingCandidate> scanAllowedMarkets() {
        return marketSelectionService.resolve(tradingProperties.getAllowedMarkets()).stream()
                .map(this::scan)
                .toList();
    }

    public TradingCandidate scan(String market) {
        try {
            List<Candle> candles = candleProvider.getRecentCandles(
                    market,
                    candidateScannerProperties.getCandleUnitMinutes(),
                    candidateScannerProperties.getCandleCount()
            );
            VolatilitySnapshot snapshot = volatilityIndicatorService.calculate(candles);
            return toCandidate(snapshot);
        } catch (RuntimeException exception) {
            log.warn("Candidate scan failed. market={}, reason={}", market, failureReason(exception));
            return new TradingCandidate(
                    market,
                    CandidateDecision.SKIPPED,
                    "Candidate scan failed: " + failureReason(exception),
                    null,
                    null,
                    null,
                    null,
                    null,
                    Instant.now()
            );
        }
    }

    private String failureReason(RuntimeException exception) {
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
        if (snapshot.trend() != MarketTrend.UP) {
            return skipped(snapshot, "Trend is not UP");
        }
        if (snapshot.priceChangeRate().compareTo(strategyMarketSettingsService.minPriceChangeRate(snapshot.market())) < 0) {
            return skipped(snapshot, "Price change rate is below threshold");
        }
        if (snapshot.tradeAmountChangeRate().compareTo(strategyMarketSettingsService.minTradeAmountChangeRate(snapshot.market())) < 0) {
            return skipped(snapshot, "Trade amount change rate is below threshold");
        }
        if (snapshot.priceChangeRate().compareTo(strategyMarketSettingsService.maxPriceChangeRate(snapshot.market())) > 0) {
            return skipped(snapshot, "Price change rate is overheated");
        }
        if (snapshot.highLowRangeRate().compareTo(strategyMarketSettingsService.maxHighLowRangeRate(snapshot.market())) > 0) {
            return skipped(snapshot, "High low range rate is overheated");
        }
        return new TradingCandidate(
                snapshot.market(),
                CandidateDecision.SELECTED,
                "Volatility long candidate selected",
                snapshot.currentPrice(),
                snapshot.priceChangeRate(),
                snapshot.highLowRangeRate(),
                snapshot.tradeAmountChangeRate(),
                snapshot.trend(),
                Instant.now()
        );
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
                Instant.now()
        );
    }
}
