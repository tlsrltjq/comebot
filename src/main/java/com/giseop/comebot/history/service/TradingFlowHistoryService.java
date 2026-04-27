package com.giseop.comebot.history.service;

import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.history.repository.TradingFlowHistoryRepository;
import com.giseop.comebot.trading.service.TradingFlowResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TradingFlowHistoryService {

    private final TradingFlowHistoryRepository tradingFlowHistoryRepository;

    public TradingFlowHistoryService(TradingFlowHistoryRepository tradingFlowHistoryRepository) {
        this.tradingFlowHistoryRepository = tradingFlowHistoryRepository;
    }

    public TradingFlowHistory save(TradingFlowResult result) {
        TradingFlowHistory history = new TradingFlowHistory(
                UUID.randomUUID().toString(),
                result.market(),
                result.currentPrice(),
                result.signalType(),
                result.signalReason(),
                result.orderCreated(),
                result.orderStatus(),
                result.message(),
                Instant.now()
        );
        return tradingFlowHistoryRepository.save(history);
    }

    public List<TradingFlowHistory> findRecent(int limit) {
        return tradingFlowHistoryRepository.findRecent(limit);
    }

    public List<TradingFlowHistory> findRecent(String market, int limit) {
        if (market == null) {
            return findRecent(limit);
        }
        return tradingFlowHistoryRepository.findRecentByMarket(market, limit);
    }

    public Optional<TradingFlowHistory> findById(String id) {
        return tradingFlowHistoryRepository.findById(id);
    }
}
