package com.giseop.comebot.history.repository;

import com.giseop.comebot.history.domain.TradingFlowHistory;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TradingFlowHistoryRepository {

    TradingFlowHistory save(TradingFlowHistory history);

    List<TradingFlowHistory> findRecent(int limit);

    List<TradingFlowHistory> findRecentByMarket(String market, int limit);

    List<TradingFlowHistory> findSince(Instant from);

    Optional<TradingFlowHistory> findById(String id);
}
