package com.giseop.comebot.backtest;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.market.candle.domain.Candle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class VolumeSurgeContinuationBacktestTest {

    @Test
    void selectsBullishVolumeSurgeAndTakesProfit() {
        CandleSeries quiet = CandleSeries.ofCandles("KRW-QUIET", 1, candles("KRW-QUIET", false));
        CandleSeries surge = CandleSeries.ofCandles("KRW-SURGE", 1, candles("KRW-SURGE", true));

        BacktestEngine.Result result = new VolumeSurgeContinuationBacktest(
                List.of(quiet, surge),
                config(5, 4.0, 1.0, 3.0, -2.0, 10)
        ).run();

        assertThat(result.signals()).isPositive();
        assertThat(result.closed()).isNotEmpty();
        assertThat(result.closed().getFirst().market()).isEqualTo("KRW-SURGE");
        assertThat(result.closed().getFirst().exitReason()).isEqualTo("TP");
    }

    @Test
    void ignoresVolumeSurgeWithoutBullishContinuationCandle() {
        CandleSeries bearish = CandleSeries.ofCandles("KRW-BEAR", 1, bearishSurge("KRW-BEAR"));

        BacktestEngine.Result result = new VolumeSurgeContinuationBacktest(
                List.of(bearish),
                config(5, 4.0, 1.0, 3.0, -2.0, 10)
        ).run();

        assertThat(result.signals()).isZero();
        assertThat(result.closed()).isEmpty();
    }

    private static VolumeSurgeContinuationConfig config(
            int averageWindow,
            double minVolumeRatio,
            double minCandleReturnPct,
            double takeProfitPct,
            double stopLossPct,
            int maxHold
    ) {
        return new VolumeSurgeContinuationConfig(
                averageWindow,
                minVolumeRatio,
                minCandleReturnPct,
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

    private static List<Candle> candles(String market, boolean includeSurge) {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            double open = 100 + i * 0.1;
            double close = open + 0.05;
            double amount = 1_000_000;
            if (includeSurge && i == 6) {
                close = open * 1.02;
                amount = 8_000_000;
            }
            if (includeSurge && i > 6) {
                close = open * 1.04;
            }
            candles.add(candle(market, i, open, close, amount));
        }
        return candles;
    }

    private static List<Candle> bearishSurge(String market) {
        List<Candle> candles = candles(market, false);
        candles.set(6, candle(market, 6, 101, 99, 8_000_000));
        return candles;
    }

    private static Candle candle(String market, int minute, double open, double close, double amount) {
        return new Candle(
                market,
                Instant.ofEpochSecond(1_700_000_000L + (long) minute * 60L),
                BigDecimal.valueOf(open),
                BigDecimal.valueOf(Math.max(open, close) + 0.1),
                BigDecimal.valueOf(Math.min(open, close) - 0.1),
                BigDecimal.valueOf(close),
                BigDecimal.valueOf(amount),
                BigDecimal.valueOf(amount / close));
    }
}
