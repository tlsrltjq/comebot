package com.giseop.comebot.strategy.candidate;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.candle.domain.Candle;
import com.giseop.comebot.market.candle.provider.BinanceCandleProvider;
import com.giseop.comebot.market.candle.provider.CandleProvider;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SessionVolatilityBreakoutScannerService {

    private static final Logger log = LoggerFactory.getLogger(SessionVolatilityBreakoutScannerService.class);
    private static final int SCALE = 8;

    private final SessionVolatilityBreakoutProperties properties;
    private final CandleProvider candleProvider;
    private final Clock clock;

    @Autowired
    public SessionVolatilityBreakoutScannerService(SessionVolatilityBreakoutProperties properties) {
        this(properties, new BinanceCandleProvider(), Clock.systemUTC());
    }

    SessionVolatilityBreakoutScannerService(
            SessionVolatilityBreakoutProperties properties,
            CandleProvider candleProvider,
            Clock clock
    ) {
        this.properties = properties;
        this.candleProvider = candleProvider;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public TradingCandidate scan(ExchangeMode exchange, String market) {
        if (exchange != ExchangeMode.BINANCE) {
            return skipped(market, "Session volatility breakout is Binance only");
        }
        if (isExcludedMarket(market)) {
            return skipped(market, "Market is excluded from session volatility universe");
        }

        try {
            List<Candle> candles = candleProvider.getRecentCandles(
                    market,
                    properties.getSignalUnitMinutes(),
                    properties.getCandleCount()
            );
            List<Candle> completedCandles = completedCandles(candles);
            int required = Math.max(properties.getBreakoutWindow(), properties.getAverageWindow()) + 1;
            if (completedCandles.size() < required) {
                return skipped(market, "Not enough completed session volatility candles");
            }

            Candle current = completedCandles.getLast();
            if (!isInSession(current)) {
                return skipped(market, "Outside UTC session window");
            }
            List<Candle> previous = completedCandles.subList(0, completedCandles.size() - 1);
            List<Candle> breakoutCandles = tail(previous, properties.getBreakoutWindow());
            List<Candle> averageCandles = tail(previous, properties.getAverageWindow());

            BigDecimal priorHigh = maxHigh(breakoutCandles);
            if (current.tradePrice().compareTo(priorHigh) <= 0) {
                return skipped(market, "Close did not break prior high");
            }

            BigDecimal currentRangeRate = rangeRate(current);
            BigDecimal averageRangeRate = averageRangeRate(averageCandles);
            BigDecimal rangeRatio = ratio(currentRangeRate, averageRangeRate);
            if (rangeRatio.compareTo(properties.getMinRangeRatio()) < 0) {
                return skipped(market, "Range ratio is below session volatility threshold");
            }

            BigDecimal averageTradeAmount = averageTradeAmount(averageCandles);
            BigDecimal volumeRatio = ratio(current.accumulatedTradePrice(), averageTradeAmount);
            if (volumeRatio.compareTo(properties.getMinVolumeRatio()) < 0) {
                return skipped(market, "Volume ratio is below session volatility threshold");
            }

            BigDecimal closeLocation = closeLocation(current);
            if (closeLocation.compareTo(properties.getMinCloseLocation()) < 0) {
                return skipped(market, "Close location is below session volatility threshold");
            }

            return new TradingCandidate(
                    market,
                    CandidateDecision.SELECTED,
                    "Session volatility breakout selected: Binance 15m UTC06-12 close-limit",
                    current.tradePrice(),
                    percentChange(current.tradePrice(), priorHigh),
                    currentRangeRate,
                    volumeRatio.multiply(new BigDecimal("100")).setScale(4, RoundingMode.HALF_UP),
                    MarketTrend.UP,
                    current.tradePrice().compareTo(current.openingPrice()) > 0,
                    Instant.now(clock)
            );
        } catch (RuntimeException exception) {
            log.warn("Session volatility breakout scan failed. market={}, reason={}", market, failureReason(exception));
            return skipped(market, "Session volatility breakout scan failed: " + failureReason(exception));
        }
    }

    private List<Candle> completedCandles(List<Candle> candles) {
        if (candles == null || candles.isEmpty()) {
            return List.of();
        }
        List<Candle> ordered = candles.stream()
                .filter(this::hasRequiredValues)
                .sorted(Comparator.comparing(Candle::candleTime))
                .toList();
        if (ordered.size() <= 1) {
            return ordered;
        }
        Candle latest = ordered.getLast();
        Instant latestCloseTime = latest.candleTime().plus(Duration.ofMinutes(properties.getSignalUnitMinutes()));
        if (Instant.now(clock).isBefore(latestCloseTime)) {
            return ordered.subList(0, ordered.size() - 1);
        }
        return ordered;
    }

    private boolean hasRequiredValues(Candle candle) {
        return candle != null
                && candle.candleTime() != null
                && positive(candle.openingPrice())
                && positive(candle.highPrice())
                && positive(candle.lowPrice())
                && positive(candle.tradePrice())
                && positive(candle.accumulatedTradePrice());
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean isInSession(Candle current) {
        int hour = current.candleTime()
                .plus(Duration.ofMinutes(properties.getSignalUnitMinutes()))
                .atZone(ZoneOffset.UTC)
                .getHour();
        return hour >= properties.getSessionStartHourUtc()
                && hour < properties.getSessionEndHourUtc();
    }

    private boolean isExcludedMarket(String market) {
        if (market == null) {
            return false;
        }
        String normalized = market.trim().toUpperCase(Locale.ROOT);
        return properties.getExcludedMarkets().stream()
                .filter(candidate -> candidate != null)
                .map(candidate -> candidate.trim().toUpperCase(Locale.ROOT))
                .anyMatch(normalized::equals);
    }

    private List<Candle> tail(List<Candle> candles, int count) {
        return candles.subList(Math.max(0, candles.size() - count), candles.size());
    }

    private BigDecimal maxHigh(List<Candle> candles) {
        return candles.stream()
                .map(Candle::highPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal averageRangeRate(List<Candle> candles) {
        BigDecimal sum = candles.stream()
                .map(this::rangeRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(candles.size()), SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal averageTradeAmount(List<Candle> candles) {
        BigDecimal sum = candles.stream()
                .map(Candle::accumulatedTradePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(candles.size()), SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal rangeRate(Candle candle) {
        return candle.highPrice()
                .subtract(candle.lowPrice())
                .multiply(new BigDecimal("100"))
                .divide(candle.openingPrice(), SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal closeLocation(Candle candle) {
        BigDecimal range = candle.highPrice().subtract(candle.lowPrice());
        if (range.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return candle.tradePrice()
                .subtract(candle.lowPrice())
                .multiply(new BigDecimal("100"))
                .divide(range, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal percentChange(BigDecimal current, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(base)
                .multiply(new BigDecimal("100"))
                .divide(base, 4, RoundingMode.HALF_UP);
    }

    private TradingCandidate skipped(String market, String reason) {
        return new TradingCandidate(
                market,
                CandidateDecision.SKIPPED,
                reason,
                null, null, null, null, null, null,
                Instant.now(clock)
        );
    }

    private String failureReason(Throwable exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getClass().getSimpleName() + " - " + exception.getMessage();
    }
}
