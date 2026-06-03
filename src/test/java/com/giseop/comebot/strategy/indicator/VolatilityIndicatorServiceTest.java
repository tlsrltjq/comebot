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
    void detectsBullishLastCandle() {
        VolatilitySnapshot snapshot = service.calculate(List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "90", "105", "1000", "10"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "100", "120", "1500", "12")
        ));

        assertThat(snapshot.lastCandleBullish()).isTrue();
    }

    @Test
    void detectsBearishLastCandle() {
        VolatilitySnapshot snapshot = service.calculate(List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "90", "105", "1000", "10"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "120", "125", "100", "110", "1500", "12")
        ));

        assertThat(snapshot.lastCandleBullish()).isFalse();
    }

    @Test
    void calculatesVolumeCooldownRatioWhenLatestIsLowerThanPeak() {
        // peak trade amount = 2000 (second candle), latest = 500 (third candle)
        // ratio = 500/2000 = 0.25
        VolatilitySnapshot snapshot = service.calculate(List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "90", "105", "1000", "10"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "122", "2000", "18"),
                candle("KRW-BTC", "2026-04-30T00:02:00Z", "122", "123", "120", "121", "500", "4")
        ));

        assertThat(snapshot.volumeCooldownRatio()).isEqualByComparingTo("0.2500");
    }

    @Test
    void countsConsecutiveBullishCandlesAtEnd() {
        // candles: bearish, bullish, bullish → 2 consecutive
        VolatilitySnapshot snapshot = service.calculate(List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "90", "95",  "800", "8"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "95",  "120", "94", "115", "1500", "14"),
                candle("KRW-BTC", "2026-04-30T00:02:00Z", "110", "118", "109", "117", "600", "5")
        ));

        assertThat(snapshot.consecutiveBullishCandles()).isEqualTo(2);
    }

    @Test
    void consecutiveBullishCountResetsByBearishCandle() {
        // candles: bullish(100→108), bearish(108→104), bullish(104→112) → 1 consecutive at end
        VolatilitySnapshot snapshot = service.calculate(List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "90",  "108", "800",  "8"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "108", "120", "107", "104", "1500", "14"),
                candle("KRW-BTC", "2026-04-30T00:02:00Z", "104", "115", "103", "112", "600",  "5")
        ));

        assertThat(snapshot.consecutiveBullishCandles()).isEqualTo(1);
    }

    @Test
    void calculatesPriceRecoveryRate() {
        // window low=90, high=120, latest close=114 → recovery=(114-90)/(120-90)*100 = 80%
        VolatilitySnapshot snapshot = service.calculate(List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "120", "90", "105", "1000", "10"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "118", "104", "114", "800", "7")
        ));

        assertThat(snapshot.priceRecoveryRate()).isEqualByComparingTo("80.0000");
    }

    @Test
    void volumeCooldownRatioIsOneWhenLatestIsThePeak() {
        // latest and peak are the same candle → ratio = 1.0
        VolatilitySnapshot snapshot = service.calculate(List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "90", "105", "500", "5"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "2000", "18")
        ));

        assertThat(snapshot.volumeCooldownRatio()).isEqualByComparingTo("1.0000");
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
