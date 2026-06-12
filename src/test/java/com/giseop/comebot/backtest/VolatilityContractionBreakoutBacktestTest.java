package com.giseop.comebot.backtest;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.market.candle.domain.Candle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class VolatilityContractionBreakoutBacktestTest {

    @Test
    void selectsBreakoutAfterContractionAndTakesProfit() {
        CandleSeries flat = CandleSeries.ofCandles("KRW-FLAT", 1, candles("KRW-FLAT", false));
        CandleSeries breakout = CandleSeries.ofCandles("KRW-BREAK", 1, candles("KRW-BREAK", true));

        BacktestEngine.Result result = new VolatilityContractionBreakoutBacktest(
                List.of(flat, breakout),
                config(6, 0.8, 6, 0.7, 3.0, -2.0, 10)
        ).run();

        assertThat(result.signals()).isPositive();
        assertThat(result.closed()).isNotEmpty();
        assertThat(result.closed().getFirst().market()).isEqualTo("KRW-BREAK");
        assertThat(result.closed().getFirst().exitReason()).isEqualTo("TP");
    }

    @Test
    void ignoresWideRangeBeforeBreakout() {
        CandleSeries wide = CandleSeries.ofCandles("KRW-WIDE", 1, wideRangeCandles("KRW-WIDE"));

        BacktestEngine.Result result = new VolatilityContractionBreakoutBacktest(
                List.of(wide),
                config(6, 0.5, 6, 0.7, 3.0, -2.0, 10)
        ).run();

        assertThat(result.signals()).isZero();
        assertThat(result.closed()).isEmpty();
    }

    private static VolatilityContractionBreakoutConfig config(
            int contractionWindow,
            double maxAverageRangePct,
            int breakoutWindow,
            double minBreakoutPct,
            double takeProfitPct,
            double stopLossPct,
            int maxHold
    ) {
        return new VolatilityContractionBreakoutConfig(
                contractionWindow,
                maxAverageRangePct,
                breakoutWindow,
                minBreakoutPct,
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

    private static List<Candle> candles(String market, boolean includeBreakout) {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            double open = 100.0;
            double close = 100.05;
            double high = 100.2;
            double low = 99.9;
            if (includeBreakout && i == 8) {
                close = 101.2;
                high = 101.4;
            }
            if (includeBreakout && i > 8) {
                close = 104.5;
                high = 104.8;
            }
            candles.add(candle(market, i, open, high, low, close));
        }
        return candles;
    }

    private static List<Candle> wideRangeCandles(String market) {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            double open = 100.0;
            double close = i == 8 ? 101.2 : 100.0;
            candles.add(candle(market, i, open, 103.0, 97.0, close));
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
