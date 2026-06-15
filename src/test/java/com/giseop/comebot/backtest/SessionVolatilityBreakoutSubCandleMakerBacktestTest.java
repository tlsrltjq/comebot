package com.giseop.comebot.backtest;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.market.candle.domain.Candle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SessionVolatilityBreakoutSubCandleMakerBacktestTest {

    @Test
    void fillsWhenOneMinuteCandleTouchesLimitWithinFiveMinutes() {
        CandleSeries signal = CandleSeries.ofCandles("ZECUSDT", 15, signalCandles("ZECUSDT"));
        CandleSeries fill = CandleSeries.ofCandles("ZECUSDT", 1, fillCandles("ZECUSDT", true));

        BacktestEngine.Result result = new SessionVolatilityBreakoutSubCandleMakerBacktest(
                List.of(signal),
                List.of(fill),
                config(),
                300
        ).run();

        assertThat(result.signals()).isPositive();
        assertThat(result.fills()).isEqualTo(1);
        assertThat(result.expiries()).isZero();
        assertThat(result.closed()).isNotEmpty();
    }

    @Test
    void expiresWhenOneMinuteCandlesDoNotTouchLimitWithinFiveMinutes() {
        CandleSeries signal = CandleSeries.ofCandles("ZECUSDT", 15, signalCandles("ZECUSDT"));
        CandleSeries fill = CandleSeries.ofCandles("ZECUSDT", 1, fillCandles("ZECUSDT", false));

        BacktestEngine.Result result = new SessionVolatilityBreakoutSubCandleMakerBacktest(
                List.of(signal),
                List.of(fill),
                config(),
                300
        ).run();

        assertThat(result.signals()).isPositive();
        assertThat(result.fills()).isZero();
        assertThat(result.expiries()).isPositive();
        assertThat(result.closed()).isEmpty();
    }

    private static SessionVolatilityBreakoutConfig config() {
        return new SessionVolatilityBreakoutConfig(
                4,
                4,
                2.0,
                2.0,
                70.0,
                0,
                24,
                4.0,
                -2.0,
                8,
                1,
                10d,
                0,
                0,
                0,
                Long.MAX_VALUE,
                1_000d);
    }

    private static List<Candle> signalCandles(String market) {
        List<Candle> candles = new ArrayList<>();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        for (int i = 0; i < 14; i++) {
            double open = 100.0;
            double high = 100.6;
            double low = 99.8;
            double close = 100.1;
            double amount = 1_000.0;
            if (i == 6) {
                open = 100.0;
                high = 108.0;
                low = 99.5;
                close = 107.0;
                amount = 8_000.0;
            }
            if (i == 8) {
                high = 112.0;
            }
            candles.add(candle(market, start.plusSeconds((long) i * 15L * 60L),
                    open, high, low, close, amount));
        }
        return candles;
    }

    private static List<Candle> fillCandles(String market, boolean touchesLimit) {
        List<Candle> candles = new ArrayList<>();
        Instant start = Instant.parse("2026-01-01T01:45:00Z");
        for (int i = 0; i < 5; i++) {
            double low = touchesLimit && i == 2 ? 106.9 : 107.1;
            candles.add(candle(market, start.plusSeconds((long) i * 60L),
                    108.0, 108.2, low, 108.0, 1_000.0));
        }
        return candles;
    }

    private static Candle candle(
            String market,
            Instant time,
            double open,
            double high,
            double low,
            double close,
            double amount
    ) {
        return new Candle(
                market,
                time,
                BigDecimal.valueOf(open),
                BigDecimal.valueOf(high),
                BigDecimal.valueOf(low),
                BigDecimal.valueOf(close),
                BigDecimal.valueOf(amount),
                BigDecimal.valueOf(10));
    }
}
