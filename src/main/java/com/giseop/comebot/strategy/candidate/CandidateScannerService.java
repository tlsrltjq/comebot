package com.giseop.comebot.strategy.candidate;

import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.market.candle.domain.Candle;
import com.giseop.comebot.market.candle.provider.CandleProvider;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import com.giseop.comebot.strategy.indicator.VolatilityIndicatorService;
import com.giseop.comebot.strategy.indicator.VolatilitySnapshot;
import com.giseop.comebot.strategy.service.StrategyMarketSettingsService;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CandidateScannerService {

    private final TradingProperties tradingProperties;
    private final CandidateScannerProperties candidateScannerProperties;
    private final CandleProvider candleProvider;
    private final VolatilityIndicatorService volatilityIndicatorService;
    private final StrategyMarketSettingsService strategyMarketSettingsService;

    public CandidateScannerService(
            TradingProperties tradingProperties,
            CandidateScannerProperties candidateScannerProperties,
            CandleProvider candleProvider,
            VolatilityIndicatorService volatilityIndicatorService,
            StrategyMarketSettingsService strategyMarketSettingsService
    ) {
        this.tradingProperties = tradingProperties;
        this.candidateScannerProperties = candidateScannerProperties;
        this.candleProvider = candleProvider;
        this.volatilityIndicatorService = volatilityIndicatorService;
        this.strategyMarketSettingsService = strategyMarketSettingsService;
    }

    public List<TradingCandidate> scanAllowedMarkets() {
        return tradingProperties.getAllowedMarkets().stream()
                .filter(market -> market != null && !market.isBlank())
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
            return new TradingCandidate(
                    market,
                    CandidateDecision.SKIPPED,
                    "Candidate scan failed: " + exception.getClass().getSimpleName(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    Instant.now()
            );
        }
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
