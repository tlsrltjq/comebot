package com.giseop.comebot.strategy.indicator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.giseop.comebot.market.candle.domain.Candle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class VolatilityIndicatorServiceTest {

    private final VolatilityIndicatorService service = new VolatilityIndicatorService();

    @Test
    void calculatesPriceChangeRate() {
        VolatilitySnapshot snapshot = service.calculate(List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "90", "105", "1000", "10"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "100", "120", "1500", "12")
        ));

        assertThat(snapshot.priceChangeRate()).isEqualByComparingTo("20.0000");
        assertThat(snapshot.trend()).isEqualTo(MarketTrend.UP);
    }

    @Test
    void calculatesHighLowRangeRate() {
        VolatilitySnapshot snapshot = service.calculate(List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "90", "105", "1000", "10"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "100", "120", "1500", "12")
        ));

        assertThat(snapshot.highLowRangeRate()).isEqualByComparingTo("38.8889");
    }

    @Test
    void calculatesTradeAmountChangeRate() {
        VolatilitySnapshot snapshot = service.calculate(List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "90", "105", "1000", "10"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "100", "120", "1500", "12")
        ));

        assertThat(snapshot.tradeAmountChangeRate()).isEqualByComparingTo("50.0000");
    }

    @Test
    void detectsDownTrend() {
        VolatilitySnapshot snapshot = service.calculate(List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "90", "95", "1000", "10"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "95", "100", "80", "90", "900", "9")
        ));

        assertThat(snapshot.trend()).isEqualTo(MarketTrend.DOWN);
    }

    @Test
    void detectsSidewaysTrend() {
        VolatilitySnapshot snapshot = service.calculate(List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "90", "100", "1000", "10"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "100", "105", "95", "100", "1000", "10")
        ));

        assertThat(snapshot.trend()).isEqualTo(MarketTrend.SIDEWAYS);
    }

    @Test
    void sortsCandlesByTimeBeforeCalculation() {
        VolatilitySnapshot snapshot = service.calculate(List.of(
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "100", "120", "1500", "12"),
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "90", "105", "1000", "10")
        ));

        assertThat(snapshot.priceChangeRate()).isEqualByComparingTo("20.0000");
        assertThat(snapshot.candleCount()).isEqualTo(2);
    }

    @Test
    void requiresAtLeastTwoCandles() {
        assertThatThrownBy(() -> service.calculate(List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "90", "105", "1000", "10")
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("at least two candles are required");
    }

    @Test
    void rejectsInvalidPrice() {
        assertThatThrownBy(() -> service.calculate(List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "0", "110", "90", "105", "1000", "10"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "100", "120", "1500", "12")
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("openingPrice must be positive");
    }

    private Candle candle(
            String market,
            String candleTime,
            String openingPrice,
            String highPrice,
            String lowPrice,
            String tradePrice,
            String accumulatedTradePrice,
            String accumulatedTradeVolume
    ) {
        return new Candle(
                market,
                Instant.parse(candleTime),
                new BigDecimal(openingPrice),
                new BigDecimal(highPrice),
                new BigDecimal(lowPrice),
                new BigDecimal(tradePrice),
                new BigDecimal(accumulatedTradePrice),
                new BigDecimal(accumulatedTradeVolume)
        );
    }
}
