package com.giseop.comebot.strategy.indicator;

import com.giseop.comebot.market.candle.domain.Candle;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VolatilityIndicatorService {

    private static final int RATE_SCALE = 4;

    public VolatilitySnapshot calculate(List<Candle> candles) {
        if (candles == null || candles.size() < 2) {
            throw new IllegalArgumentException("at least two candles are required");
        }

        List<Candle> orderedCandles = candles.stream()
                .sorted(Comparator.comparing(Candle::candleTime))
                .toList();

        Candle oldest = orderedCandles.getFirst();
        Candle latest = orderedCandles.getLast();

        validateCandle(oldest);
        validateCandle(latest);

        BigDecimal highestPrice = orderedCandles.stream()
                .map(Candle::highPrice)
                .max(BigDecimal::compareTo)
                .orElseThrow();
        BigDecimal lowestPrice = orderedCandles.stream()
                .map(Candle::lowPrice)
                .min(BigDecimal::compareTo)
                .orElseThrow();

        BigDecimal priceChangeRate = rate(latest.tradePrice().subtract(oldest.openingPrice()), oldest.openingPrice());
        BigDecimal highLowRangeRate = rate(highestPrice.subtract(lowestPrice), lowestPrice);
        BigDecimal tradeAmountChangeRate = rate(
                latest.accumulatedTradePrice().subtract(oldest.accumulatedTradePrice()),
                oldest.accumulatedTradePrice()
        );

        boolean lastCandleBullish = latest.tradePrice().compareTo(latest.openingPrice()) > 0;

        return new VolatilitySnapshot(
                latest.market(),
                latest.tradePrice(),
                priceChangeRate,
                highLowRangeRate,
                tradeAmountChangeRate,
                trend(priceChangeRate),
                orderedCandles.size(),
                lastCandleBullish,
                latest.accumulatedTradePrice()
        );
    }

    private void validateCandle(Candle candle) {
        if (candle == null || candle.market() == null || candle.market().isBlank()) {
            throw new IllegalArgumentException("candle market must not be blank");
        }
        validatePositive(candle.openingPrice(), "openingPrice");
        validatePositive(candle.highPrice(), "highPrice");
        validatePositive(candle.lowPrice(), "lowPrice");
        validatePositive(candle.tradePrice(), "tradePrice");
        validatePositive(candle.accumulatedTradePrice(), "accumulatedTradePrice");
    }

    private void validatePositive(BigDecimal value, String name) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private BigDecimal rate(BigDecimal diff, BigDecimal base) {
        return diff.multiply(new BigDecimal("100"))
                .divide(base, RATE_SCALE, RoundingMode.HALF_UP);
    }

    private MarketTrend trend(BigDecimal priceChangeRate) {
        int compared = priceChangeRate.compareTo(BigDecimal.ZERO);
        if (compared > 0) {
            return MarketTrend.UP;
        }
        if (compared < 0) {
            return MarketTrend.DOWN;
        }
        return MarketTrend.SIDEWAYS;
    }
}
