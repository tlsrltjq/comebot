package com.giseop.comebot.history.repository;

import com.giseop.comebot.history.domain.TradingFlowHistory;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "history.storage-type", havingValue = "IN_MEMORY", matchIfMissing = true)
public class InMemoryTradingFlowHistoryRepository implements TradingFlowHistoryRepository {

    private final ConcurrentHashMap<String, TradingFlowHistory> histories = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<String> recentIds = new ConcurrentLinkedDeque<>();

    // Test-only in-memory storage. Data is lost when the application restarts.
    @Override
    public TradingFlowHistory save(TradingFlowHistory history) {
        histories.put(history.id(), history);
        recentIds.remove(history.id());
        recentIds.addFirst(history.id());
        return history;
    }

    @Override
    public List<TradingFlowHistory> findRecent(int limit) {
        return recentIds.stream()
                .limit(limit)
                .map(histories::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<TradingFlowHistory> findRecentByMarket(String market, int limit) {
        return recentIds.stream()
                .map(histories::get)
                .filter(Objects::nonNull)
                .filter(history -> market.equals(history.market()))
                .limit(limit)
                .toList();
    }

    @Override
    public List<TradingFlowHistory> findSince(Instant from) {
        return recentIds.stream()
                .map(histories::get)
                .filter(Objects::nonNull)
                .filter(history -> !history.createdAt().isBefore(from))
                .toList();
    }

    @Override
    public Optional<TradingFlowHistory> findById(String id) {
        return Optional.ofNullable(histories.get(id));
    }
}
