package com.giseop.comebot.backtest;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.market.candle.domain.Candle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RankedRotationBacktestTest {

    @Test
    void entersTopRankedMarketsAndTakesProfit() {
        CandleSeries weak = CandleSeries.ofCandles("KRW-WEAK", 1, candles("KRW-WEAK", 0.1, false));
        CandleSeries strong = CandleSeries.ofCandles("KRW-STRONG", 1, candles("KRW-STRONG", 0.8, true));

        BacktestEngine.Result result = new RankedRotationBacktest(
                List.of(weak, strong),
                config(6, 2.0, 1, 6, 3.0, -2.0, 20)
        ).run();

        assertThat(result.signals()).isPositive();
        assertThat(result.closed()).isNotEmpty();
        assertThat(result.closed().getFirst().market()).isEqualTo("KRW-STRONG");
        assertThat(result.closed().getFirst().exitReason()).isEqualTo("TP");
    }

    @Test
    void rotatesOutMarketThatLeavesTopRank() {
        CandleSeries early = CandleSeries.ofCandles("KRW-EARLY", 1, rotationCandles("KRW-EARLY", true));
        CandleSeries late = CandleSeries.ofCandles("KRW-LATE", 1, rotationCandles("KRW-LATE", false));

        BacktestEngine.Result result = new RankedRotationBacktest(
                List.of(early, late),
                config(4, 0.5, 1, 4, 20.0, -20.0, 100)
        ).run();

        assertThat(result.closed())
                .anySatisfy(trade -> {
                    assertThat(trade.market()).isEqualTo("KRW-EARLY");
                    assertThat(trade.exitReason()).isEqualTo("ROTATE");
                });
    }

    private static RankedRotationConfig config(
            int lookback,
            double minReturn,
            int rankCount,
            int rebalanceEvery,
            double takeProfitPct,
            double stopLossPct,
            int maxHold
    ) {
        return new RankedRotationConfig(
                lookback,
                minReturn,
                rankCount,
                rebalanceEvery,
                takeProfitPct,
                stopLossPct,
                maxHold,
                10_000d,
                0,
                0,
                0,
                Long.MAX_VALUE,
                1_000_000d);
    }

    private static List<Candle> candles(String market, double step, boolean breakoutAfterEntry) {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            double close = 100.0 + i * step;
            double high = breakoutAfterEntry && i > 7 ? 110.0 : close + 0.2;
            candles.add(candle(market, i, close - 0.1, high, close - 0.3, close));
        }
        return candles;
    }

    private static List<Candle> rotationCandles(String market, boolean earlyLeader) {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            double close;
            if (earlyLeader) {
                close = i < 8 ? 100.0 + i : 108.0;
            } else {
                close = i < 8 ? 100.0 : 100.0 + (i - 7) * 2.0;
            }
            candles.add(candle(market, i, close - 0.1, close + 0.2, close - 0.3, close));
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
