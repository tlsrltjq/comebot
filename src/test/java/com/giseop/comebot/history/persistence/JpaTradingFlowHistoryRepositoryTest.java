package com.giseop.comebot.history.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.strategy.domain.SignalType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class JpaTradingFlowHistoryRepositoryTest {

    private SpringDataTradingFlowHistoryJpaRepository springDataRepository;
    private JpaTradingFlowHistoryRepository repository;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataTradingFlowHistoryJpaRepository.class);
        repository = new JpaTradingFlowHistoryRepository(springDataRepository);
    }

    @Test
    void entityUsesTradingFlowHistoryTableMapping() {
        assertThat(TradingFlowHistoryEntity.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(TradingFlowHistoryEntity.class.getAnnotation(Table.class).name())
                .isEqualTo("trading_flow_history");
    }

    @Test
    void saveMapsDomainToEntityAndReturnsStoredHistory() {
        TradingFlowHistory history = history("history-1", "KRW-BTC", Instant.parse("2026-04-27T00:00:00Z"));
        when(springDataRepository.save(any(TradingFlowHistoryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TradingFlowHistory saved = repository.save(history);

        assertThat(saved).isEqualTo(history);
    }

    @Test
    void findRecentReturnsStoredHistoryInRepositoryOrder() {
        TradingFlowHistory oldHistory = history("history-1", "KRW-BTC", Instant.parse("2026-04-27T00:00:00Z"));
        TradingFlowHistory newHistory = history("history-2", "KRW-ETH", Instant.parse("2026-04-27T00:01:00Z"));
        when(springDataRepository.findByExchangeOrderByCreatedAtDesc(ExchangeMode.UPBIT, PageRequest.of(0, 20)))
                .thenReturn(List.of(
                        TradingFlowHistoryEntity.from(newHistory),
                        TradingFlowHistoryEntity.from(oldHistory)
                ));

        assertThat(repository.findRecent(20)).containsExactly(newHistory, oldHistory);
    }

    @Test
    void findByIdReturnsStoredHistory() {
        TradingFlowHistory history = history("history-1", "KRW-BTC", Instant.parse("2026-04-27T00:00:00Z"));
        when(springDataRepository.findById("history-1"))
                .thenReturn(Optional.of(TradingFlowHistoryEntity.from(history)));

        assertThat(repository.findById("history-1")).contains(history);
    }

    @Test
    void findRecentByMarketReturnsOnlyMatchingMarket() {
        TradingFlowHistory btcOld = history("history-1", "KRW-BTC", Instant.parse("2026-04-27T00:00:00Z"));
        TradingFlowHistory btcNew = history("history-3", "KRW-BTC", Instant.parse("2026-04-27T00:02:00Z"));
        when(springDataRepository.findByExchangeAndMarketOrderByCreatedAtDesc(ExchangeMode.UPBIT, "KRW-BTC", PageRequest.of(0, 20)))
                .thenReturn(List.of(
                        TradingFlowHistoryEntity.from(btcNew),
                        TradingFlowHistoryEntity.from(btcOld)
                ));
        when(springDataRepository.findByExchangeAndMarketOrderByCreatedAtDesc(ExchangeMode.UPBIT, "KRW-XRP", PageRequest.of(0, 20)))
                .thenReturn(List.of());

        assertThat(repository.findRecentByMarket("KRW-BTC", 20)).containsExactly(btcNew, btcOld);
        assertThat(repository.findRecentByMarket("KRW-XRP", 20)).isEmpty();
    }

    @Test
    void findSinceReturnsHistoriesCreatedAfterStartTime() {
        Instant from = Instant.parse("2026-04-29T00:00:00Z");
        TradingFlowHistory history = history("history-1", "KRW-BTC", from);
        when(springDataRepository.findByExchangeAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(ExchangeMode.UPBIT, from))
                .thenReturn(List.of(TradingFlowHistoryEntity.from(history)));

        assertThat(repository.findSince(from)).containsExactly(history);
    }

    private TradingFlowHistory history(String id, String market, Instant createdAt) {
        return new TradingFlowHistory(
                id,
                market,
                new BigDecimal("100000000.00000000"),
                SignalType.BUY,
                "Test threshold buy signal",
                true,
                OrderStatus.FILLED,
                "Paper trading order filled",
                createdAt
        );
    }
}
