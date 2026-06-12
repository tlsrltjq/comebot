package com.giseop.comebot.backtest;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.market.candle.domain.Candle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RelativeStrengthMomentumBacktestTest {

    @Test
    void selectsStrongestMarketAndTakesProfit() {
        CandleSeries weak = CandleSeries.ofCandles("KRW-WEAK", 1, rising("KRW-WEAK", 100, 0.1));
        CandleSeries strong = CandleSeries.ofCandles("KRW-STRONG", 1, rising("KRW-STRONG", 100, 1.0));

        BacktestEngine.Result result = new RelativeStrengthMomentumBacktest(
                List.of(weak, strong),
                config(3, 2.0, 3.0, -2.0, 10)
        ).run();

        assertThat(result.signals()).isPositive();
        assertThat(result.closed()).isNotEmpty();
        assertThat(result.closed().getFirst().market()).isEqualTo("KRW-STRONG");
        assertThat(result.closed().getFirst().exitReason()).isEqualTo("TP");
        assertThat(BacktestDecisionPolicy.decide(result)).startsWith("reject:");
    }

    @Test
    void doesNotSignalBelowMinimumReturn() {
        CandleSeries flat = CandleSeries.ofCandles("KRW-FLAT", 1, rising("KRW-FLAT", 100, 0.05));

        BacktestEngine.Result result = new RelativeStrengthMomentumBacktest(
                List.of(flat),
                config(5, 10.0, 3.0, -2.0, 10)
        ).run();

        assertThat(result.signals()).isZero();
        assertThat(result.closed()).isEmpty();
    }

    private static RelativeStrengthMomentumConfig config(
            int lookback,
            double minReturnPct,
            double takeProfitPct,
            double stopLossPct,
            int maxHold
    ) {
        return new RelativeStrengthMomentumConfig(
                lookback,
                minReturnPct,
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

    private static List<Candle> rising(String market, double start, double step) {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            double open = start + i * step;
            double close = open + step;
            candles.add(new Candle(
                    market,
                    Instant.ofEpochSecond(1_700_000_000L + (long) i * 60L),
                    BigDecimal.valueOf(open),
                    BigDecimal.valueOf(close + 0.1),
                    BigDecimal.valueOf(open - 0.1),
                    BigDecimal.valueOf(close),
                    BigDecimal.valueOf(1_000_000),
                    BigDecimal.valueOf(10)));
        }
        return candles;
    }
}
