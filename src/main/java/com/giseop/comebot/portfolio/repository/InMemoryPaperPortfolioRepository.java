package com.giseop.comebot.portfolio.repository;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.portfolio.domain.PaperPortfolio;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.domain.PaperRealizedProfit;
import com.giseop.comebot.portfolio.domain.PaperTradeLog;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "paper.portfolio-storage-type", havingValue = "IN_MEMORY", matchIfMissing = true)
public class InMemoryPaperPortfolioRepository implements PaperPortfolioRepository {

    private final Map<ExchangeMode, PortfolioState> portfolios = new ConcurrentHashMap<>();

    @Override
    public synchronized BigDecimal getCash(ExchangeMode exchange) {
        return state(exchange).cash;
    }

    @Override
    public synchronized void saveCash(ExchangeMode exchange, BigDecimal cash) {
        state(exchange).cash = cash;
    }

    @Override
    public synchronized BigDecimal getRealizedProfit(ExchangeMode exchange) {
        return state(exchange).realizedProfit;
    }

    @Override
    public synchronized void saveRealizedProfit(ExchangeMode exchange, BigDecimal realizedProfit) {
        state(exchange).realizedProfit = realizedProfit;
    }

    @Override
    public void saveRealizedProfitEvent(ExchangeMode exchange, PaperRealizedProfit realizedProfit) {
        state(exchange).realizedProfitEvents.addFirst(realizedProfit);
    }

    @Override
    public void saveTradeLog(ExchangeMode exchange, PaperTradeLog tradeLog) {
        state(exchange).tradeLogs.addFirst(tradeLog);
    }

    @Override
    public List<PaperRealizedProfit> findRealizedProfitsSince(ExchangeMode exchange, Instant from) {
        return state(exchange).realizedProfitEvents.stream()
                .filter(event -> !event.realizedAt().isBefore(from))
                .toList();
    }

    @Override
    public Optional<PaperPosition> findPosition(ExchangeMode exchange, String market) {
        return Optional.ofNullable(state(exchange).positions.get(market));
    }

    @Override
    public List<PaperPosition> findPositions(ExchangeMode exchange) {
        return state(exchange).positions.values().stream()
                .filter(position -> position.quantity().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(PaperPosition::market))
                .toList();
    }

    @Override
    public void savePosition(ExchangeMode exchange, PaperPosition position) {
        state(exchange).positions.put(position.market(), position);
    }

    @Override
    public synchronized PaperPortfolio getPortfolio(ExchangeMode exchange) {
        PortfolioState state = state(exchange);
        return new PaperPortfolio(
                exchange,
                PaperPortfolio.currencyFor(exchange),
                state.cash,
                state.realizedProfit,
                findPositions(exchange)
        );
    }

    private PortfolioState state(ExchangeMode exchange) {
        ExchangeMode key = exchange == null ? ExchangeMode.UPBIT : exchange;
        return portfolios.computeIfAbsent(key, ignored -> new PortfolioState());
    }

    private static final class PortfolioState {
        private final Map<String, PaperPosition> positions = new ConcurrentHashMap<>();
        private final ConcurrentLinkedDeque<PaperRealizedProfit> realizedProfitEvents = new ConcurrentLinkedDeque<>();
        private final ConcurrentLinkedDeque<PaperTradeLog> tradeLogs = new ConcurrentLinkedDeque<>();
        private BigDecimal cash = BigDecimal.ZERO;
        private BigDecimal realizedProfit = BigDecimal.ZERO;
    }
}
