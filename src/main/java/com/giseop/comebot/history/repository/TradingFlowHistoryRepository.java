package com.giseop.comebot.history.repository;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.history.domain.TradingFlowHistory;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TradingFlowHistoryRepository {

    TradingFlowHistory save(TradingFlowHistory history);

    default List<TradingFlowHistory> findRecent(int limit) {
        return findRecent(ExchangeMode.UPBIT, limit);
    }

    List<TradingFlowHistory> findRecent(ExchangeMode exchange, int limit);

    default List<TradingFlowHistory> findRecentByMarket(String market, int limit) {
        return findRecentByMarket(ExchangeMode.UPBIT, market, limit);
    }

    List<TradingFlowHistory> findRecentByMarket(ExchangeMode exchange, String market, int limit);

    default List<TradingFlowHistory> findSince(Instant from) {
        return findSince(ExchangeMode.UPBIT, from);
    }

    List<TradingFlowHistory> findSince(ExchangeMode exchange, Instant from);

    Optional<TradingFlowHistory> findById(String id);
}
