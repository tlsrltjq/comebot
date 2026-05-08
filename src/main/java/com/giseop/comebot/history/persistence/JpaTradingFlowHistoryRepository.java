package com.giseop.comebot.history.persistence;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.history.repository.TradingFlowHistoryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "history.storage-type", havingValue = "JPA")
public class JpaTradingFlowHistoryRepository implements TradingFlowHistoryRepository {

    private final SpringDataTradingFlowHistoryJpaRepository jpaRepository;

    public JpaTradingFlowHistoryRepository(SpringDataTradingFlowHistoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TradingFlowHistory save(TradingFlowHistory history) {
        return jpaRepository.save(TradingFlowHistoryEntity.from(history)).toDomain();
    }

    @Override
    public List<TradingFlowHistory> findRecent(ExchangeMode exchange, int limit) {
        return jpaRepository.findByExchangeOrderByCreatedAtDesc(exchange, PageRequest.of(0, limit)).stream()
                .map(TradingFlowHistoryEntity::toDomain)
                .toList();
    }

    @Override
    public List<TradingFlowHistory> findRecentByMarket(ExchangeMode exchange, String market, int limit) {
        return jpaRepository.findByExchangeAndMarketOrderByCreatedAtDesc(exchange, market, PageRequest.of(0, limit)).stream()
                .map(TradingFlowHistoryEntity::toDomain)
                .toList();
    }

    @Override
    public List<TradingFlowHistory> findSince(ExchangeMode exchange, Instant from) {
        return jpaRepository.findByExchangeAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(exchange, from).stream()
                .map(TradingFlowHistoryEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<TradingFlowHistory> findById(String id) {
        return jpaRepository.findById(id)
                .map(TradingFlowHistoryEntity::toDomain);
    }
}
