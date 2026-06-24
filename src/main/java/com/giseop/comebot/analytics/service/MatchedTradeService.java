package com.giseop.comebot.analytics.service;

import com.giseop.comebot.analytics.dto.MatchedTrade;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.portfolio.domain.PaperTradeLog;
import com.giseop.comebot.portfolio.repository.PaperPortfolioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MatchedTradeService {

    private static final int LOOKBACK_DAYS = 30;
    private static final int MAX_MATCHED = 200;

    private final PaperPortfolioRepository paperPortfolioRepository;

    public MatchedTradeService(PaperPortfolioRepository paperPortfolioRepository) {
        this.paperPortfolioRepository = paperPortfolioRepository;
    }

    public List<MatchedTrade> findRecent(ExchangeMode exchange, int limit) {
        Instant since = Instant.now().minus(LOOKBACK_DAYS, ChronoUnit.DAYS);
        List<PaperTradeLog> events = paperPortfolioRepository.findTradeLogsSince(exchange, since).stream()
                .filter(log -> log.side() == OrderSide.BUY || log.side() == OrderSide.SELL)
                .sorted(Comparator.comparing(PaperTradeLog::executedAt))
                .toList();

        Map<String, Deque<PaperTradeLog>> buysByMarket = new HashMap<>();
        List<MatchedTrade> matched = new ArrayList<>();

        for (PaperTradeLog event : events) {
            if (event.side() == OrderSide.BUY) {
                buysByMarket.computeIfAbsent(event.market(), ignored -> new ArrayDeque<>()).addLast(event);
            } else if (event.side() == OrderSide.SELL && event.realizedProfit() != null) {
                Deque<PaperTradeLog> buys = buysByMarket.get(event.market());
                if (buys != null && !buys.isEmpty()) {
                    matched.add(buildTrade(exchange, buys.peekLast(), event));
                }
            }
        }

        return matched.stream()
                .sorted(Comparator.comparing(MatchedTrade::sellAt).reversed())
                .limit(Math.min(limit, MAX_MATCHED))
                .toList();
    }

    private MatchedTrade buildTrade(ExchangeMode exchange, PaperTradeLog buy, PaperTradeLog sell) {
        return new MatchedTrade(
                exchange,
                sell.market(),
                buy.executedAt(),
                valueOrZero(buy.price()),
                sell.executedAt(),
                valueOrZero(sell.price()),
                ChronoUnit.SECONDS.between(buy.executedAt(), sell.executedAt()),
                profitRate(sell),
                classifyExitReason(sell)
        );
    }

    private BigDecimal profitRate(PaperTradeLog sell) {
        BigDecimal realizedProfit = sell.realizedProfit();
        BigDecimal grossAmount = sell.grossAmount();
        if (realizedProfit == null || grossAmount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal costBasis = grossAmount.subtract(realizedProfit);
        if (costBasis.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return realizedProfit
                .divide(costBasis, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private String classifyExitReason(PaperTradeLog sell) {
        BigDecimal realizedProfit = sell.realizedProfit();
        if (realizedProfit == null) {
            return "MANUAL";
        }
        if (realizedProfit.compareTo(BigDecimal.ZERO) > 0) {
            return "TAKE_PROFIT";
        }
        if (realizedProfit.compareTo(BigDecimal.ZERO) < 0) {
            return "STOP_LOSS";
        }
        return "MANUAL";
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
