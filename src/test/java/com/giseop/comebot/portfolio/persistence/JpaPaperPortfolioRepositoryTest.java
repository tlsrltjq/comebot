package com.giseop.comebot.portfolio.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.portfolio.domain.PaperPortfolio;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.domain.PaperRealizedProfit;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JpaPaperPortfolioRepositoryTest {

    private SpringDataPaperPortfolioStateJpaRepository stateRepository;
    private SpringDataPaperPositionJpaRepository positionRepository;
    private SpringDataPaperRealizedProfitEventJpaRepository eventRepository;
    private JpaPaperPortfolioRepository repository;

    @BeforeEach
    void setUp() {
        stateRepository = mock(SpringDataPaperPortfolioStateJpaRepository.class);
        positionRepository = mock(SpringDataPaperPositionJpaRepository.class);
        eventRepository = mock(SpringDataPaperRealizedProfitEventJpaRepository.class);
        repository = new JpaPaperPortfolioRepository(stateRepository, positionRepository, eventRepository);
    }

    @Test
    void entitiesUseExpectedTableNames() {
        assertThat(PaperPortfolioStateEntity.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(PaperPortfolioStateEntity.class.getAnnotation(Table.class).name()).isEqualTo("paper_portfolio_state");
        assertThat(PaperPositionEntity.class.getAnnotation(Table.class).name()).isEqualTo("paper_position");
        assertThat(PaperRealizedProfitEventEntity.class.getAnnotation(Table.class).name()).isEqualTo("paper_realized_profit_event");
    }

    @Test
    void missingStateReturnsZeroCashAndProfit() {
        when(stateRepository.findById(ExchangeMode.UPBIT)).thenReturn(Optional.empty());

        assertThat(repository.getCash(ExchangeMode.UPBIT)).isEqualByComparingTo("0");
        assertThat(repository.getRealizedProfit(ExchangeMode.UPBIT)).isEqualByComparingTo("0");
    }

    @Test
    void saveCashCreatesStateWhenMissing() {
        when(stateRepository.findById(ExchangeMode.BINANCE)).thenReturn(Optional.empty());
        when(stateRepository.save(any(PaperPortfolioStateEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        repository.saveCash(ExchangeMode.BINANCE, new BigDecimal("1000"));

        verify(stateRepository).save(any(PaperPortfolioStateEntity.class));
    }

    @Test
    void findPositionMapsEntityToDomain() {
        PaperPosition position = new PaperPosition("BTCUSDT", new BigDecimal("0.01"), new BigDecimal("50000"));
        when(positionRepository.findByExchangeAndMarket(ExchangeMode.BINANCE, "BTCUSDT"))
                .thenReturn(Optional.of(PaperPositionEntity.from(ExchangeMode.BINANCE, position)));

        assertThat(repository.findPosition(ExchangeMode.BINANCE, "BTCUSDT")).contains(position);
    }

    @Test
    void findPositionsReturnsOnlyPositivePositionsFromSpringRepository() {
        PaperPosition btc = new PaperPosition("KRW-BTC", new BigDecimal("0.01"), new BigDecimal("100000000"));
        when(positionRepository.findByExchangeAndQuantityGreaterThanOrderByMarketAsc(ExchangeMode.UPBIT, BigDecimal.ZERO))
                .thenReturn(List.of(PaperPositionEntity.from(ExchangeMode.UPBIT, btc)));

        assertThat(repository.findPositions(ExchangeMode.UPBIT)).containsExactly(btc);
    }

    @Test
    void getPortfolioCombinesStateAndPositions() {
        PaperPortfolioStateEntity state = new PaperPortfolioStateEntity(
                ExchangeMode.BINANCE,
                new BigDecimal("990"),
                new BigDecimal("1.5")
        );
        PaperPosition position = new PaperPosition("ETHUSDT", new BigDecimal("1"), new BigDecimal("10"));
        when(stateRepository.findById(ExchangeMode.BINANCE)).thenReturn(Optional.of(state));
        when(positionRepository.findByExchangeAndQuantityGreaterThanOrderByMarketAsc(ExchangeMode.BINANCE, BigDecimal.ZERO))
                .thenReturn(List.of(PaperPositionEntity.from(ExchangeMode.BINANCE, position)));

        PaperPortfolio portfolio = repository.getPortfolio(ExchangeMode.BINANCE);

        assertThat(portfolio.exchange()).isEqualTo(ExchangeMode.BINANCE);
        assertThat(portfolio.currency()).isEqualTo("USDT");
        assertThat(portfolio.cash()).isEqualByComparingTo("990");
        assertThat(portfolio.realizedProfit()).isEqualByComparingTo("1.5");
        assertThat(portfolio.positions()).containsExactly(position);
    }

    @Test
    void realizedProfitEventsAreFilteredByExchangeAndTime() {
        Instant from = Instant.parse("2026-05-09T00:00:00Z");
        PaperRealizedProfit event = new PaperRealizedProfit(new BigDecimal("-10"), from);
        when(eventRepository.findByExchangeAndRealizedAtGreaterThanEqualOrderByRealizedAtDesc(ExchangeMode.UPBIT, from))
                .thenReturn(List.of(PaperRealizedProfitEventEntity.from(ExchangeMode.UPBIT, event)));

        assertThat(repository.findRealizedProfitsSince(ExchangeMode.UPBIT, from)).containsExactly(event);
    }
}
