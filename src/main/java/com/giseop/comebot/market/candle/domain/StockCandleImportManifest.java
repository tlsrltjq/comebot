package com.giseop.comebot.market.candle.domain;

import com.giseop.comebot.market.domain.MarketAssetClass;
import com.giseop.comebot.market.domain.MarketIdentity;
import com.giseop.comebot.market.domain.MarketVenue;
import java.nio.file.Path;
import java.time.Instant;

public record StockCandleImportManifest(
        String provider,
        MarketIdentity identity,
        CandleInterval interval,
        String timezone,
        boolean regularSessionOnly,
        boolean adjusted,
        Instant since,
        Instant until,
        Instant collectedAt,
        Path dataFile
) {

    public StockCandleImportManifest {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }
        if (identity == null) {
            throw new IllegalArgumentException("identity must not be null");
        }
        if (identity.assetClass() != MarketAssetClass.STOCK || identity.venue() != MarketVenue.US_STOCK) {
            throw new IllegalArgumentException("identity must be a US stock market");
        }
        if (interval == null) {
            throw new IllegalArgumentException("interval must not be null");
        }
        if (timezone == null || timezone.isBlank()) {
            throw new IllegalArgumentException("timezone must not be blank");
        }
        if (!identity.timezone().equals(timezone)) {
            throw new IllegalArgumentException("timezone must match market identity venue");
        }
        if (since == null || until == null || collectedAt == null) {
            throw new IllegalArgumentException("time fields must not be null");
        }
        if (!since.isBefore(until)) {
            throw new IllegalArgumentException("since must be before until");
        }
        if (dataFile == null) {
            throw new IllegalArgumentException("dataFile must not be null");
        }
        provider = provider.trim();
    }

    public String expectedRelativePath() {
        return Path.of(
                "stock",
                "us",
                provider,
                interval.code(),
                identity.symbol() + ".csv"
        ).toString();
    }
}
