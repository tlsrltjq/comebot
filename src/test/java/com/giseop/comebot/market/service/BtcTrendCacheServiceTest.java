package com.giseop.comebot.market.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.market.candle.domain.Candle;
import com.giseop.comebot.market.candle.provider.CandleProvider;
import com.giseop.comebot.market.service.BtcTrendCacheService.BtcTrend;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class BtcTrendCacheServiceTest {

    private static final List<Candle> RISING = List.of(
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

    private static final List<Candle> FALLING = List.of(
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

    private static Candle candle(String time, String price) {
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

    private static BtcTrendCacheService service(CandleProvider provider) {
        return new BtcTrendCacheService(provider);
    }

    @Test
    void initialTrendIsNeutral() {
        BtcTrendCacheService svc = service((market, unit, count) -> List.of());

        assertThat(svc.trend()).isEqualTo(BtcTrend.NEUTRAL);
    }

    @Test
    void notEnoughCandlesKeepsPreviousTrend() {
        // Need at least EMA_LONG=10 candles; provide fewer
        BtcTrendCacheService svc = service((market, unit, count) -> List.of(
                candle("2026-01-01T00:00:00Z", "100"),
                candle("2026-01-01T01:00:00Z", "101")
        ));
        svc.refresh();

        assertThat(svc.trend()).isEqualTo(BtcTrend.NEUTRAL);
    }

    @Test
    void risingPricesShouldResultInUpTrend() {
        BtcTrendCacheService svc = service((market, unit, count) -> RISING);
        svc.refresh();

        assertThat(svc.trend()).isEqualTo(BtcTrend.UP);
    }

    @Test
    void fallingPricesShouldResultInDownTrend() {
        BtcTrendCacheService svc = service((market, unit, count) -> FALLING);
        svc.refresh();

        assertThat(svc.trend()).isEqualTo(BtcTrend.DOWN);
    }

    @Test
    void refreshFailureDoesNotChangeTrend() {
        BtcTrendCacheService svc = service((market, unit, count) -> {
            throw new RuntimeException("network error");
        });
        svc.refresh();

        assertThat(svc.trend()).isEqualTo(BtcTrend.NEUTRAL);
    }

    @Test
    void candlesWithZeroPriceAreFiltered() {
        // Zero-price candle prepended; effective 12 rising candles remain after filter
        List<Candle> candlesWithZero = new java.util.ArrayList<>();
        candlesWithZero.add(candle("2026-01-01T00:00:00Z", "0"));
        candlesWithZero.addAll(RISING);
        BtcTrendCacheService svc = service((market, unit, count) -> candlesWithZero);
        svc.refresh();

        assertThat(svc.trend()).isEqualTo(BtcTrend.UP);
    }

    @Test
    void trendUpdatesOnSubsequentRefresh() {
        boolean[] useFalling = {false};
        BtcTrendCacheService svc = service((market, unit, count) -> useFalling[0] ? FALLING : RISING);

        svc.refresh();
        assertThat(svc.trend()).isEqualTo(BtcTrend.UP);

        useFalling[0] = true;
        svc.refresh();
        assertThat(svc.trend()).isEqualTo(BtcTrend.DOWN);
    }
}
