package com.giseop.comebot.history.service;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.history.repository.TradingFlowHistoryRepository;
import com.giseop.comebot.strategy.domain.SignalType;
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
        return save(ExchangeMode.UPBIT, result);
    }

    public TradingFlowHistory save(ExchangeMode exchange, TradingFlowResult result) {
        if (result.signalType() == SignalType.HOLD) {
            return null;
        }
        TradingFlowHistory history = new TradingFlowHistory(
                UUID.randomUUID().toString(),
                exchange,
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
        return findRecent(ExchangeMode.UPBIT, limit);
    }

    public List<TradingFlowHistory> findRecent(ExchangeMode exchange, int limit) {
        return tradingFlowHistoryRepository.findRecent(exchange, limit);
    }

    public List<TradingFlowHistory> findRecent(String market, int limit) {
        return findRecent(ExchangeMode.UPBIT, market, limit);
    }

    public List<TradingFlowHistory> findRecent(ExchangeMode exchange, String market, int limit) {
        if (market == null) {
            return findRecent(exchange, limit);
        }
        return tradingFlowHistoryRepository.findRecentByMarket(exchange, market, limit);
    }

    public List<TradingFlowHistory> findSince(Instant from) {
        return findSince(ExchangeMode.UPBIT, from);
    }

    public List<TradingFlowHistory> findSince(ExchangeMode exchange, Instant from) {
        return tradingFlowHistoryRepository.findSince(exchange, from);
    }

    public Optional<TradingFlowHistory> findById(String id) {
        return tradingFlowHistoryRepository.findById(id);
    }
}
