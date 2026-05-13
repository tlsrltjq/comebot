package com.giseop.comebot.portfolio.persistence;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.portfolio.domain.PaperPortfolio;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.domain.PaperRealizedProfit;
import com.giseop.comebot.portfolio.repository.PaperPortfolioRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "paper.portfolio-storage-type", havingValue = "JPA")
public class JpaPaperPortfolioRepository implements PaperPortfolioRepository {

    private final SpringDataPaperPortfolioStateJpaRepository stateRepository;
    private final SpringDataPaperPositionJpaRepository positionRepository;
    private final SpringDataPaperRealizedProfitEventJpaRepository realizedProfitEventRepository;

    public JpaPaperPortfolioRepository(
            SpringDataPaperPortfolioStateJpaRepository stateRepository,
            SpringDataPaperPositionJpaRepository positionRepository,
            SpringDataPaperRealizedProfitEventJpaRepository realizedProfitEventRepository
    ) {
        this.stateRepository = stateRepository;
        this.positionRepository = positionRepository;
        this.realizedProfitEventRepository = realizedProfitEventRepository;
    }

    @Override
    public BigDecimal getCash(ExchangeMode exchange) {
        return state(exchange).getCash();
    }

    @Override
    public void saveCash(ExchangeMode exchange, BigDecimal cash) {
        PaperPortfolioStateEntity state = state(exchange);
        state.setCash(cash);
        stateRepository.save(state);
    }

    @Override
    public BigDecimal getRealizedProfit(ExchangeMode exchange) {
        return state(exchange).getRealizedProfit();
    }

    @Override
    public void saveRealizedProfit(ExchangeMode exchange, BigDecimal realizedProfit) {
        PaperPortfolioStateEntity state = state(exchange);
        state.setRealizedProfit(realizedProfit);
        stateRepository.save(state);
    }

    @Override
    public void saveRealizedProfitEvent(ExchangeMode exchange, PaperRealizedProfit realizedProfit) {
        realizedProfitEventRepository.save(PaperRealizedProfitEventEntity.from(exchange, realizedProfit));
    }

    @Override
    public List<PaperRealizedProfit> findRealizedProfitsSince(ExchangeMode exchange, Instant from) {
        return realizedProfitEventRepository.findByExchangeAndRealizedAtGreaterThanEqualOrderByRealizedAtDesc(exchangeOf(exchange), from)
                .stream()
                .map(PaperRealizedProfitEventEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<PaperPosition> findPosition(ExchangeMode exchange, String market) {
        return positionRepository.findByExchangeAndMarket(exchangeOf(exchange), market)
                .map(PaperPositionEntity::toDomain);
    }

    @Override
    public List<PaperPosition> findPositions(ExchangeMode exchange) {
        return positionRepository.findByExchangeAndQuantityGreaterThanOrderByMarketAsc(exchangeOf(exchange), BigDecimal.ZERO)
                .stream()
                .map(PaperPositionEntity::toDomain)
                .toList();
    }

    @Override
    public void savePosition(ExchangeMode exchange, PaperPosition position) {
        positionRepository.save(PaperPositionEntity.from(exchangeOf(exchange), position));
    }

    @Override
    public PaperPortfolio getPortfolio(ExchangeMode exchange) {
        ExchangeMode mode = exchangeOf(exchange);
        PaperPortfolioStateEntity state = state(mode);
        return new PaperPortfolio(
                mode,
                PaperPortfolio.currencyFor(mode),
                state.getCash(),
                state.getRealizedProfit(),
                findPositions(mode)
        );
    }

    private PaperPortfolioStateEntity state(ExchangeMode exchange) {
        ExchangeMode mode = exchangeOf(exchange);
        return stateRepository.findById(mode)
                .orElseGet(() -> new PaperPortfolioStateEntity(mode, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private ExchangeMode exchangeOf(ExchangeMode exchange) {
        return exchange == null ? ExchangeMode.UPBIT : exchange;
    }
}
