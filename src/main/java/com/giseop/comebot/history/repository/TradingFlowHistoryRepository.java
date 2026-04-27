package com.giseop.comebot.history.repository;

import com.giseop.comebot.history.domain.TradingFlowHistory;
import java.util.List;
import java.util.Optional;

public interface TradingFlowHistoryRepository {

    TradingFlowHistory save(TradingFlowHistory history);

    List<TradingFlowHistory> findRecent(int limit);

    List<TradingFlowHistory> findRecentByMarket(String market, int limit);

    Optional<TradingFlowHistory> findById(String id);
}
