package com.giseop.comebot.market.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.market.candle.domain.Candle;
import com.giseop.comebot.market.candle.provider.UpbitCandleProvider;
import com.giseop.comebot.market.service.BtcTrendCacheService.BtcTrend;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class BtcTrendCacheServiceTest {

    // Stub that returns a fixed candle list
    private static class StubUpbitCandleProvider extends UpbitCandleProvider {

        List<Candle> candles = List.of();

        StubUpbitCandleProvider() {
            super(); // uses public no-arg constructor; getRecentCandles is overridden below
        }

        @Override
        public List<Candle> getRecentCandles(String market, int unitMinutes, int count) {
            return candles;
        }
    }

    private Candle candle(String time, String price) {
        return new Candle(
                "KRW-BTC",
                Instant.parse(time),
                new BigDecimal(price),
                new BigDecimal(price),
                new BigDecimal(price),
                new BigDecimal(price),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    @Test
    void initialTrendIsNeutral() {
        StubUpbitCandleProvider stub = new StubUpbitCandleProvider();
        stub.candles = List.of();
        BtcTrendCacheService service = new BtcTrendCacheService(stub);

        assertThat(service.trend()).isEqualTo(BtcTrend.NEUTRAL);
    }

    @Test
    void notEnoughCandlesKeepsPreviousTrend() {
        StubUpbitCandleProvider stub = new StubUpbitCandleProvider();
        // Need at least EMA_LONG=10 candles; provide fewer
        stub.candles = List.of(
                candle("2026-01-01T00:00:00Z", "100"),
                candle("2026-01-01T01:00:00Z", "101")
        );
        BtcTrendCacheService service = new BtcTrendCacheService(stub);
        service.refresh();

        // Should remain NEUTRAL (initial) because not enough candles
        assertThat(service.trend()).isEqualTo(BtcTrend.NEUTRAL);
    }

    @Test
    void risingPricesShouldResultInUpTrend() {
        StubUpbitCandleProvider stub = new StubUpbitCandleProvider();
        // Steadily rising prices: short EMA > long EMA → UP
        stub.candles = List.of(
                candle("2026-01-01T00:00:00Z", "90000"),
                candle("2026-01-01T01:00:00Z", "91000"),
                candle("2026-01-01T02:00:00Z", "92000"),
                candle("2026-01-01T03:00:00Z", "93000"),
                candle("2026-01-01T04:00:00Z", "94000"),
                candle("2026-01-01T05:00:00Z", "95000"),
                candle("2026-01-01T06:00:00Z", "96000"),
                candle("2026-01-01T07:00:00Z", "97000"),
                candle("2026-01-01T08:00:00Z", "98000"),
                candle("2026-01-01T09:00:00Z", "99000"),
                candle("2026-01-01T10:00:00Z", "100000"),
                candle("2026-01-01T11:00:00Z", "101000")
        );
        BtcTrendCacheService service = new BtcTrendCacheService(stub);
        service.refresh();

        assertThat(service.trend()).isEqualTo(BtcTrend.UP);
    }

    @Test
    void fallingPricesShouldResultInDownTrend() {
        StubUpbitCandleProvider stub = new StubUpbitCandleProvider();
        // Steadily falling prices: short EMA < long EMA → DOWN
        stub.candles = List.of(
                candle("2026-01-01T00:00:00Z", "100000"),
                candle("2026-01-01T01:00:00Z", "99000"),
                candle("2026-01-01T02:00:00Z", "98000"),
                candle("2026-01-01T03:00:00Z", "97000"),
                candle("2026-01-01T04:00:00Z", "96000"),
                candle("2026-01-01T05:00:00Z", "95000"),
                candle("2026-01-01T06:00:00Z", "94000"),
                candle("2026-01-01T07:00:00Z", "93000"),
                candle("2026-01-01T08:00:00Z", "92000"),
                candle("2026-01-01T09:00:00Z", "91000"),
                candle("2026-01-01T10:00:00Z", "90000"),
                candle("2026-01-01T11:00:00Z", "89000")
        );
        BtcTrendCacheService service = new BtcTrendCacheService(stub);
        service.refresh();

        assertThat(service.trend()).isEqualTo(BtcTrend.DOWN);
    }

    @Test
    void refreshFailureDoesNotChangeTrend() {
        StubUpbitCandleProvider stub = new StubUpbitCandleProvider() {
            private boolean called = false;

            @Override
            public List<Candle> getRecentCandles(String market, int unitMinutes, int count) {
                if (!called) {
                    called = true;
                    return candles; // first call returns candles for bootstrap
                }
                throw new RuntimeException("network error");
            }
        };
        stub.candles = List.of(); // not enough candles → stays NEUTRAL after bootstrap
        BtcTrendCacheService service = new BtcTrendCacheService(stub);
        // Initial is NEUTRAL; refresh failure should not change it
        service.refresh();

        assertThat(service.trend()).isEqualTo(BtcTrend.NEUTRAL);
    }

    @Test
    void candlesWithZeroPriceAreFiltered() {
        StubUpbitCandleProvider stub = new StubUpbitCandleProvider();
        // Mix zero-price candles with valid rising ones — effective count must still reach EMA_LONG=10
        stub.candles = List.of(
                candle("2026-01-01T00:00:00Z", "0"),   // filtered out
                candle("2026-01-01T01:00:00Z", "90000"),
                candle("2026-01-01T02:00:00Z", "91000"),
                candle("2026-01-01T03:00:00Z", "92000"),
                candle("2026-01-01T04:00:00Z", "93000"),
                candle("2026-01-01T05:00:00Z", "94000"),
                candle("2026-01-01T06:00:00Z", "95000"),
                candle("2026-01-01T07:00:00Z", "96000"),
                candle("2026-01-01T08:00:00Z", "97000"),
                candle("2026-01-01T09:00:00Z", "98000"),
                candle("2026-01-01T10:00:00Z", "99000"),
                candle("2026-01-01T11:00:00Z", "100000")
        );
        BtcTrendCacheService service = new BtcTrendCacheService(stub);
        service.refresh();

        assertThat(service.trend()).isEqualTo(BtcTrend.UP);
    }

    @Test
    void trendUpdatesOnSubsequentRefresh() {
        StubUpbitCandleProvider stub = new StubUpbitCandleProvider();
        // First refresh: rising → UP
        stub.candles = List.of(
                candle("2026-01-01T00:00:00Z", "90000"),
                candle("2026-01-01T01:00:00Z", "91000"),
                candle("2026-01-01T02:00:00Z", "92000"),
                candle("2026-01-01T03:00:00Z", "93000"),
                candle("2026-01-01T04:00:00Z", "94000"),
                candle("2026-01-01T05:00:00Z", "95000"),
                candle("2026-01-01T06:00:00Z", "96000"),
                candle("2026-01-01T07:00:00Z", "97000"),
                candle("2026-01-01T08:00:00Z", "98000"),
                candle("2026-01-01T09:00:00Z", "99000"),
                candle("2026-01-01T10:00:00Z", "100000"),
                candle("2026-01-01T11:00:00Z", "101000")
        );
        BtcTrendCacheService service = new BtcTrendCacheService(stub);
        service.refresh();
        assertThat(service.trend()).isEqualTo(BtcTrend.UP);

        // Second refresh: falling → DOWN
        stub.candles = List.of(
                candle("2026-01-02T00:00:00Z", "100000"),
                candle("2026-01-02T01:00:00Z", "99000"),
                candle("2026-01-02T02:00:00Z", "98000"),
                candle("2026-01-02T03:00:00Z", "97000"),
                candle("2026-01-02T04:00:00Z", "96000"),
                candle("2026-01-02T05:00:00Z", "95000"),
                candle("2026-01-02T06:00:00Z", "94000"),
                candle("2026-01-02T07:00:00Z", "93000"),
                candle("2026-01-02T08:00:00Z", "92000"),
                candle("2026-01-02T09:00:00Z", "91000"),
                candle("2026-01-02T10:00:00Z", "90000"),
                candle("2026-01-02T11:00:00Z", "89000")
        );
        service.refresh();
        assertThat(service.trend()).isEqualTo(BtcTrend.DOWN);
    }
}
