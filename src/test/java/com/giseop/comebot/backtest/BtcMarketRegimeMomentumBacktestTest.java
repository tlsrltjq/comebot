package com.giseop.comebot.backtest;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.market.candle.domain.Candle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BtcMarketRegimeMomentumBacktestTest {

    @Test
    void selectsMarketOnlyWhenBtcAndMarketRegimePass() {
        CandleSeries btc = CandleSeries.ofCandles("KRW-BTC", 1, btcUpCandles("KRW-BTC"));
        CandleSeries eth = CandleSeries.ofCandles("KRW-ETH", 1, ethMomentumCandles("KRW-ETH"));

        BacktestEngine.Result result = new BtcMarketRegimeMomentumBacktest(
                List.of(btc, eth),
                config(6, 2.0, 6, 3.0, 3.0, -2.0, 10)
        ).run();

        assertThat(result.signals()).isPositive();
        assertThat(result.closed()).isNotEmpty();
        assertThat(result.closed().getFirst().market()).isEqualTo("KRW-ETH");
        assertThat(result.closed().getFirst().exitReason()).isEqualTo("TP");
    }

    @Test
    void skipsWhenBtcRegimeDoesNotPass() {
        CandleSeries btc = CandleSeries.ofCandles("KRW-BTC", 1, flatCandles("KRW-BTC"));
        CandleSeries eth = CandleSeries.ofCandles("KRW-ETH", 1, ethMomentumCandles("KRW-ETH"));

        BacktestEngine.Result result = new BtcMarketRegimeMomentumBacktest(
                List.of(btc, eth),
                config(6, 2.0, 6, 3.0, 3.0, -2.0, 10)
        ).run();

        assertThat(result.signals()).isZero();
        assertThat(result.closed()).isEmpty();
    }

    private static BtcMarketRegimeMomentumConfig config(
            int btcLookback,
            double minBtcReturn,
            int marketLookback,
            double minMarketReturn,
            double takeProfitPct,
            double stopLossPct,
            int maxHold
    ) {
        return new BtcMarketRegimeMomentumConfig(
                btcLookback,
                minBtcReturn,
                marketLookback,
                minMarketReturn,
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

    private static List<Candle> btcUpCandles(String market) {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            double close = 100.0 + i * 0.6;
            candles.add(candle(market, i, close - 0.1, close + 0.2, close - 0.3, close));
        }
        return candles;
    }

    private static List<Candle> flatCandles(String market) {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            candles.add(candle(market, i, 100.0, 100.2, 99.8, 100.0));
        }
        return candles;
    }

    private static List<Candle> ethMomentumCandles(String market) {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            double close = i < 8 ? 100.0 + i * 0.8 : 106.5;
            double high = i > 8 ? 111.0 : close + 0.2;
            candles.add(candle(market, i, close - 0.1, high, close - 0.3, close));
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
