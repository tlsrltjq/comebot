package com.giseop.comebot.history.persistence;

import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.history.repository.TradingFlowHistoryRepository;
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
    public List<TradingFlowHistory> findRecent(int limit) {
        return jpaRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).stream()
                .map(TradingFlowHistoryEntity::toDomain)
                .toList();
    }

    @Override
    public List<TradingFlowHistory> findRecentByMarket(String market, int limit) {
        return jpaRepository.findByMarketOrderByCreatedAtDesc(market, PageRequest.of(0, limit)).stream()
                .map(TradingFlowHistoryEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<TradingFlowHistory> findById(String id) {
        return jpaRepository.findById(id)
                .map(TradingFlowHistoryEntity::toDomain);
    }
}
