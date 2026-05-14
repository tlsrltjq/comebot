package com.giseop.comebot.market.service;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.domain.TickerSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class TickerSnapshotStore {

    private final ConcurrentHashMap<SnapshotKey, TickerSnapshot> snapshots = new ConcurrentHashMap<>();

    public void save(TickerSnapshot snapshot) {
        SnapshotKey key = new SnapshotKey(snapshot.exchange(), normalize(snapshot.market()));
        snapshots.compute(key, (ignored, current) -> {
            if (current == null || !snapshot.capturedAt().isBefore(current.capturedAt())) {
                return snapshot;
            }
            return current;
        });
    }

    public Optional<TickerSnapshot> find(ExchangeMode exchange, String market) {
        if (exchange == null || market == null || market.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(snapshots.get(new SnapshotKey(exchange, normalize(market))));
    }

    public Optional<TickerSnapshot> findFresh(
            ExchangeMode exchange,
            String market,
            Duration staleAfter,
            Instant now
    ) {
        return find(exchange, market)
                .filter(snapshot -> isFresh(snapshot, staleAfter, now));
    }

    public boolean isFresh(TickerSnapshot snapshot, Duration staleAfter, Instant now) {
        if (snapshot == null || staleAfter == null || now == null) {
            return false;
        }
        return !snapshot.capturedAt().plus(staleAfter).isBefore(now);
    }

    public List<TickerSnapshot> findAll(ExchangeMode exchange) {
        return snapshots.values().stream()
                .filter(snapshot -> snapshot.exchange() == exchange)
                .sorted(Comparator.comparing(TickerSnapshot::market))
                .toList();
    }

    public int count() {
        return snapshots.size();
    }

    public int count(ExchangeMode exchange) {
        if (exchange == null) {
            return 0;
        }
        return (int) snapshots.values().stream()
                .filter(snapshot -> snapshot.exchange() == exchange)
                .count();
    }

    public int countFresh(Duration staleAfter, Instant now) {
        return (int) snapshots.values().stream()
                .filter(snapshot -> isFresh(snapshot, staleAfter, now))
                .count();
    }

    public int countFresh(ExchangeMode exchange, Duration staleAfter, Instant now) {
        if (exchange == null) {
            return 0;
        }
        return (int) snapshots.values().stream()
                .filter(snapshot -> snapshot.exchange() == exchange)
                .filter(snapshot -> isFresh(snapshot, staleAfter, now))
                .count();
    }

    private String normalize(String market) {
        return market.trim().toUpperCase(Locale.ROOT);
    }

    private record SnapshotKey(
            ExchangeMode exchange,
            String market
    ) {
    }
}
