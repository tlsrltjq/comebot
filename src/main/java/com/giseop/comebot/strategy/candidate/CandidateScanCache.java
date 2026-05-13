package com.giseop.comebot.strategy.candidate;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.strategy.dto.TradingCandidateResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class CandidateScanCache {

    private static final Duration DEFAULT_TTL = Duration.ofSeconds(30);

    private final ConcurrentHashMap<CacheKey, CacheEntry> entries = new ConcurrentHashMap<>();
    private final Duration ttl;

    public CandidateScanCache() {
        this(DEFAULT_TTL);
    }

    CandidateScanCache(Duration ttl) {
        this.ttl = ttl;
    }

    public synchronized List<TradingCandidateResponse> getOrLoad(
            ExchangeMode exchange,
            int limit,
            boolean refresh,
            Supplier<List<TradingCandidateResponse>> loader
    ) {
        ExchangeMode key = exchange == null ? ExchangeMode.UPBIT : exchange;
        CacheKey cacheKey = new CacheKey(key, Math.max(1, limit));
        Instant now = Instant.now();
        CacheEntry current = entries.get(cacheKey);
        if (!refresh && current != null && current.isFresh(now, ttl)) {
            return current.responses();
        }

        List<TradingCandidateResponse> responses = List.copyOf(loader.get());
        entries.put(cacheKey, new CacheEntry(responses, Instant.now()));
        return responses;
    }

    public void clear() {
        entries.clear();
    }

    private record CacheKey(ExchangeMode exchange, int limit) {
    }

    private record CacheEntry(List<TradingCandidateResponse> responses, Instant cachedAt) {

        private boolean isFresh(Instant now, Duration ttl) {
            return cachedAt.plus(ttl).isAfter(now);
        }
    }
}
