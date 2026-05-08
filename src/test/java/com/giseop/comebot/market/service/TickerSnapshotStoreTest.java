package com.giseop.comebot.market.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.domain.PriceSource;
import com.giseop.comebot.market.domain.TickerSnapshot;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TickerSnapshotStoreTest {

    private final TickerSnapshotStore tickerSnapshotStore = new TickerSnapshotStore();

    @Test
    void saveKeepsLatestSnapshotByExchangeAndMarket() {
        Instant now = Instant.parse("2026-05-08T00:00:00Z");
        tickerSnapshotStore.save(snapshot(ExchangeMode.UPBIT, "KRW-BTC", "100", now));
        tickerSnapshotStore.save(snapshot(ExchangeMode.UPBIT, "KRW-BTC", "90", now.minusSeconds(1)));
        tickerSnapshotStore.save(snapshot(ExchangeMode.BINANCE, "BTCUSDT", "95", now.minusSeconds(1)));

        assertThat(tickerSnapshotStore.find(ExchangeMode.UPBIT, "krw-btc"))
                .hasValueSatisfying(snapshot -> assertThat(snapshot.tradePrice()).isEqualByComparingTo("100"));
        assertThat(tickerSnapshotStore.find(ExchangeMode.BINANCE, "BTCUSDT"))
                .hasValueSatisfying(snapshot -> assertThat(snapshot.tradePrice()).isEqualByComparingTo("95"));
    }

    @Test
    void findFreshReturnsOnlySnapshotsWithinStaleDuration() {
        Instant now = Instant.parse("2026-05-08T00:00:00Z");
        tickerSnapshotStore.save(snapshot(ExchangeMode.UPBIT, "KRW-BTC", "100", now.minusMillis(2999)));
        tickerSnapshotStore.save(snapshot(ExchangeMode.UPBIT, "KRW-ETH", "200", now.minusMillis(3001)));

        assertThat(tickerSnapshotStore.findFresh(ExchangeMode.UPBIT, "KRW-BTC", Duration.ofSeconds(3), now))
                .isPresent();
        assertThat(tickerSnapshotStore.findFresh(ExchangeMode.UPBIT, "KRW-ETH", Duration.ofSeconds(3), now))
                .isEmpty();
    }

    @Test
    void countSeparatesExchangeSnapshots() {
        Instant now = Instant.parse("2026-05-08T00:00:00Z");
        tickerSnapshotStore.save(snapshot(ExchangeMode.UPBIT, "KRW-BTC", "100", now));
        tickerSnapshotStore.save(snapshot(ExchangeMode.BINANCE, "BTCUSDT", "100", now));

        assertThat(tickerSnapshotStore.count()).isEqualTo(2);
        assertThat(tickerSnapshotStore.count(ExchangeMode.UPBIT)).isEqualTo(1);
        assertThat(tickerSnapshotStore.count(ExchangeMode.BINANCE)).isEqualTo(1);
    }

    private TickerSnapshot snapshot(ExchangeMode exchange, String market, String price, Instant capturedAt) {
        return new TickerSnapshot(
                exchange,
                market,
                new BigDecimal(price),
                null,
                capturedAt,
                PriceSource.WEBSOCKET
        );
    }
}
