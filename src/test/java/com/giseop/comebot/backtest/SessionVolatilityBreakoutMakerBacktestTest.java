package com.giseop.comebot.backtest;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.market.candle.domain.Candle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SessionVolatilityBreakoutMakerBacktestTest {

    @Test
    void doesNotFillOnSignalCandleAndFillsOnLaterCandle() {
        CandleSeries series = CandleSeries.ofCandles("ZECUSDT", 15, candles("ZECUSDT", true));

        BacktestEngine.Result result = new SessionVolatilityBreakoutMakerBacktest(
                List.of(series),
                config(),
                1
        ).run();

        assertThat(result.signals()).isPositive();
        assertThat(result.fills()).isEqualTo(1);
        assertThat(result.expiries()).isZero();
        assertThat(result.closed()).isNotEmpty();
        assertThat(result.closed().getFirst().market()).isEqualTo("ZECUSDT");
    }

    @Test
    void expiresWhenLaterCandleDoesNotTouchLimit() {
        CandleSeries series = CandleSeries.ofCandles("ZECUSDT", 15, candles("ZECUSDT", false));

        BacktestEngine.Result result = new SessionVolatilityBreakoutMakerBacktest(
                List.of(series),
                config(),
                1
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

    private static List<Candle> candles(String market, boolean laterTouchesLimit) {
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
            if (i == 7) {
                low = laterTouchesLimit ? 106.9 : 107.1;
                close = 108.0;
            }
            if (i == 8 && laterTouchesLimit) {
                high = 112.0;
            }
            candles.add(candle(market, start.plusSeconds((long) i * 15L * 60L),
                    open, high, low, close, amount));
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
