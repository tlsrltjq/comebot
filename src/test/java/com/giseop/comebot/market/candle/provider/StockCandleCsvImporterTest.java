package com.giseop.comebot.market.candle.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.giseop.comebot.market.candle.domain.CandleInterval;
import com.giseop.comebot.market.candle.domain.StockCandleImportManifest;
import com.giseop.comebot.market.domain.MarketIdentity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StockCandleCsvImporterTest {

    private final StockCandleCsvImporter importer = new StockCandleCsvImporter();

    @TempDir
    private Path tempDir;

    @Test
    void loadsValidStockCandleCsv() throws Exception {
        Path csv = write("""
                timestamp,open,high,low,close,volume
                2026-01-02T14:30:00Z,100,102,99,101,12345
                2026-01-02T14:45:00Z,101,103,100,102,23456
                """);

        var rows = importer.load(manifest(csv));

        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst().timestamp()).isEqualTo(Instant.parse("2026-01-02T14:30:00Z"));
        assertThat(rows.getFirst().close()).isEqualByComparingTo("101");
    }

    @Test
    void rejectsMissingRequiredColumn() throws Exception {
        Path csv = write("""
                timestamp,open,high,low,close
                2026-01-02T14:30:00Z,100,102,99,101
                """);

        assertThatThrownBy(() -> importer.load(manifest(csv)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("missing required csv column: volume");
    }

    @Test
    void rejectsDuplicateOrUnsortedTimestamps() throws Exception {
        Path csv = write("""
                timestamp,open,high,low,close,volume
                2026-01-02T14:45:00Z,101,103,100,102,23456
                2026-01-02T14:30:00Z,100,102,99,101,12345
                """);

        assertThatThrownBy(() -> importer.load(manifest(csv)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("timestamps must be strictly increasing at line 3");
    }

    @Test
    void rejectsPriceOutsideHighLowRange() throws Exception {
        Path csv = write("""
                timestamp,open,high,low,close,volume
                2026-01-02T14:30:00Z,105,102,99,101,12345
                """);

        assertThatThrownBy(() -> importer.load(manifest(csv)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("open and close must be within high-low range");
    }

    @Test
    void rejectsTimestampOutsideManifestRange() throws Exception {
        Path csv = write("""
                timestamp,open,high,low,close,volume
                2026-01-04T14:30:00Z,100,102,99,101,12345
                """);

        assertThatThrownBy(() -> importer.load(manifest(csv)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("timestamp is outside manifest range at line 2");
    }

    private Path write(String content) throws Exception {
        Path csv = tempDir.resolve("AAPL.csv");
        Files.writeString(csv, content);
        return csv;
    }

    private StockCandleImportManifest manifest(Path csv) {
        return new StockCandleImportManifest(
                "sample",
                MarketIdentity.usStock("AAPL"),
                CandleInterval.FIFTEEN_MINUTES,
                "America/New_York",
                true,
                false,
                Instant.parse("2026-01-02T00:00:00Z"),
                Instant.parse("2026-01-03T00:00:00Z"),
                Instant.parse("2026-01-04T00:00:00Z"),
                csv
        );
    }
}
