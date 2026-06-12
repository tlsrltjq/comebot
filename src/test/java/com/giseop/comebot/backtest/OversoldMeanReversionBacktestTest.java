package com.giseop.comebot.backtest;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.market.candle.domain.Candle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OversoldMeanReversionBacktestTest {

    @Test
    void selectsOversoldMarketAndTakesProfitOnBounce() {
        CandleSeries stable = CandleSeries.ofCandles("KRW-STABLE", 1, candles("KRW-STABLE", false));
        CandleSeries oversold = CandleSeries.ofCandles("KRW-OVERSOLD", 1, candles("KRW-OVERSOLD", true));

        BacktestEngine.Result result = new OversoldMeanReversionBacktest(
                List.of(stable, oversold),
                config(6, 4.0, 2.0, 3.0, 2.0, -2.5, 10)
        ).run();

        assertThat(result.signals()).isPositive();
        assertThat(result.closed()).isNotEmpty();
        assertThat(result.closed().getFirst().market()).isEqualTo("KRW-OVERSOLD");
        assertThat(result.closed().getFirst().exitReason()).isEqualTo("TP");
    }

    @Test
    void ignoresOversoldCandleWhenCurrentRangeIsTooWide() {
        CandleSeries wide = CandleSeries.ofCandles("KRW-WIDE", 1, wideRangeCandles("KRW-WIDE"));

        BacktestEngine.Result result = new OversoldMeanReversionBacktest(
                List.of(wide),
                config(6, 4.0, 2.0, 2.0, 2.0, -2.5, 10)
        ).run();

        assertThat(result.signals()).isZero();
        assertThat(result.closed()).isEmpty();
    }

    private static OversoldMeanReversionConfig config(
            int lookback,
            double minDrop,
            double minDeviation,
            double maxRange,
            double takeProfitPct,
            double stopLossPct,
            int maxHold
    ) {
        return new OversoldMeanReversionConfig(
                lookback,
                minDrop,
                minDeviation,
                maxRange,
                takeProfitPct,
                stopLossPct,
                maxHold,
                1,
                10_000d,
                0,
                0,
                0,
                Long.MAX_VALUE,
                1_000_000d);
    }

    private static List<Candle> candles(String market, boolean includeOversoldBounce) {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            double open = 100.0;
            double close = 100.0;
            double high = 100.4;
            double low = 99.8;
            if (includeOversoldBounce && i == 8) {
                open = 96.0;
                close = 94.0;
                high = 96.2;
                low = 93.8;
            }
            if (includeOversoldBounce && i > 8) {
                open = 94.2;
                close = 97.0;
                high = 98.0;
                low = 94.0;
            }
            candles.add(candle(market, i, open, high, low, close));
        }
        return candles;
    }

    private static List<Candle> wideRangeCandles(String market) {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            double open = i == 8 ? 96.0 : 100.0;
            double close = i == 8 ? 94.0 : 100.0;
            candles.add(candle(market, i, open, 101.0, 90.0, close));
        }
        return candles;
    }

    private static Candle candle(String market, int minute, double open, double high, double low, double close) {
        return new Candle(
                market,
                Instant.ofEpochSecond(1_700_000_000L + (long) minute * 60L),
                BigDecimal.valueOf(open),
                BigDecimal.valueOf(high),
                BigDecimal.valueOf(low),
                BigDecimal.valueOf(close),
                BigDecimal.valueOf(1_000_000),
                BigDecimal.valueOf(10));
    }
}
