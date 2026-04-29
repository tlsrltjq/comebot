package com.giseop.comebot.portfolio.repository;

import com.giseop.comebot.portfolio.domain.PaperPortfolio;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.domain.PaperRealizedProfit;
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

    private final Map<String, PaperPosition> positions = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<PaperRealizedProfit> realizedProfitEvents = new ConcurrentLinkedDeque<>();
    private BigDecimal cash = BigDecimal.ZERO;
    private BigDecimal realizedProfit = BigDecimal.ZERO;

    @Override
    public synchronized BigDecimal getCash() {
        return cash;
    }

    @Override
    public synchronized void saveCash(BigDecimal cash) {
        this.cash = cash;
    }

    @Override
    public synchronized BigDecimal getRealizedProfit() {
        return realizedProfit;
    }

    @Override
    public synchronized void saveRealizedProfit(BigDecimal realizedProfit) {
        this.realizedProfit = realizedProfit;
    }

    @Override
    public void saveRealizedProfitEvent(PaperRealizedProfit realizedProfit) {
        realizedProfitEvents.addFirst(realizedProfit);
    }

    @Override
    public List<PaperRealizedProfit> findRealizedProfitsSince(Instant from) {
        return realizedProfitEvents.stream()
                .filter(event -> !event.realizedAt().isBefore(from))
                .toList();
    }

    @Override
    public Optional<PaperPosition> findPosition(String market) {
        return Optional.ofNullable(positions.get(market));
    }

    @Override
    public List<PaperPosition> findPositions() {
        return positions.values().stream()
                .filter(position -> position.quantity().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(PaperPosition::market))
                .toList();
    }

    @Override
    public void savePosition(PaperPosition position) {
        positions.put(position.market(), position);
    }

    @Override
    public synchronized PaperPortfolio getPortfolio() {
        return new PaperPortfolio(cash, realizedProfit, findPositions());
    }
}
