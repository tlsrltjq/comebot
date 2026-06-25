package com.giseop.comebot.market.candle.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.giseop.comebot.market.domain.MarketIdentity;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StockCandleImportManifestTest {

    @Test
    void createsStockManifestWithExpectedCachePath() {
        StockCandleImportManifest manifest = manifest();

        assertThat(manifest.provider()).isEqualTo("sample");
        assertThat(manifest.identity()).isEqualTo(MarketIdentity.usStock("AAPL"));
        assertThat(manifest.interval()).isEqualTo(CandleInterval.FIFTEEN_MINUTES);
        assertThat(manifest.timezone()).isEqualTo("America/New_York");
        assertThat(manifest.regularSessionOnly()).isTrue();
        assertThat(manifest.adjusted()).isFalse();
        assertThat(manifest.expectedRelativePath()).isEqualTo(Path.of(
                "stock", "us", "sample", "15m", "AAPL.csv").toString());
    }

    @Test
    void rejectsCryptoIdentity() {
        assertThatThrownBy(() -> new StockCandleImportManifest(
                "sample",
                MarketIdentity.binance("BTCUSDT"),
                CandleInterval.FIFTEEN_MINUTES,
                "UTC",
                true,
                false,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"),
                Instant.parse("2026-01-03T00:00:00Z"),
                Path.of("BTCUSDT.csv")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("identity must be a US stock market");
    }

    @Test
    void rejectsTimezoneMismatch() {
        assertThatThrownBy(() -> new StockCandleImportManifest(
                "sample",
                MarketIdentity.usStock("AAPL"),
                CandleInterval.ONE_DAY,
                "UTC",
                true,
                true,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"),
                Instant.parse("2026-01-03T00:00:00Z"),
                Path.of("AAPL.csv")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("timezone must match market identity venue");
    }

    @Test
    void rejectsInvalidTimeRange() {
        assertThatThrownBy(() -> new StockCandleImportManifest(
                "sample",
                MarketIdentity.usStock("AAPL"),
                CandleInterval.ONE_DAY,
                "America/New_York",
                true,
                true,
                Instant.parse("2026-01-02T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"),
                Instant.parse("2026-01-03T00:00:00Z"),
                Path.of("AAPL.csv")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("since must be before until");
    }

    @Test
    void parsesSupportedIntervals() {
        assertThat(CandleInterval.fromCode("1m")).isEqualTo(CandleInterval.ONE_MINUTE);
        assertThat(CandleInterval.fromCode("5M")).isEqualTo(CandleInterval.FIVE_MINUTES);
        assertThat(CandleInterval.fromCode("15m")).isEqualTo(CandleInterval.FIFTEEN_MINUTES);
        assertThat(CandleInterval.fromCode("1d")).isEqualTo(CandleInterval.ONE_DAY);
    }

    private StockCandleImportManifest manifest() {
        return new StockCandleImportManifest(
                " sample ",
                MarketIdentity.usStock("AAPL"),
                CandleInterval.FIFTEEN_MINUTES,
                "America/New_York",
                true,
                false,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"),
                Instant.parse("2026-01-03T00:00:00Z"),
                Path.of(".backtest_cache", "stock", "us", "sample", "15m", "AAPL.csv")
        );
    }
}
