package com.giseop.comebot.analytics.service;

import com.giseop.comebot.analytics.dto.MatchedTrade;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.strategy.domain.SignalType;
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

/**
 * 체결된 BUY와 SELL을 마켓별로 짝지어 완성된 거래(라운드 트립)를 반환한다.
 *
 * <p>매칭 방식: 마켓별로 BUY FILLED → SELL FILLED 순서대로 선입선출(FIFO) 매칭.
 * 짝 없는 BUY (미청산 포지션)는 결과에서 제외된다.
 */
@Service
public class MatchedTradeService {

    private static final int LOOKBACK_DAYS = 30;
    private static final int MAX_MATCHED = 200;

    private final TradingFlowHistoryService historyService;

    public MatchedTradeService(TradingFlowHistoryService historyService) {
        this.historyService = historyService;
    }

    public List<MatchedTrade> findRecent(ExchangeMode exchange, int limit) {
        Instant since = Instant.now().minus(LOOKBACK_DAYS, ChronoUnit.DAYS);
        List<TradingFlowHistory> events = historyService.findSince(exchange, since).stream()
                .filter(h -> h.orderStatus() == OrderStatus.FILLED
                        && (h.signalType() == SignalType.BUY || h.signalType() == SignalType.SELL))
                .sorted(Comparator.comparing(TradingFlowHistory::createdAt))
                .toList();

        // FIFO queue per market: BUY 이벤트를 쌓고 SELL이 오면 꺼내서 매칭
        Map<String, Deque<TradingFlowHistory>> pendingBuys = new HashMap<>();
        List<MatchedTrade> matched = new ArrayList<>();

        for (TradingFlowHistory event : events) {
            String market = event.market();
            if (event.signalType() == SignalType.BUY) {
                pendingBuys.computeIfAbsent(market, k -> new ArrayDeque<>()).addLast(event);
            } else if (event.signalType() == SignalType.SELL) {
                Deque<TradingFlowHistory> queue = pendingBuys.get(market);
                if (queue != null && !queue.isEmpty()) {
                    TradingFlowHistory buy = queue.pollFirst();
                    matched.add(buildTrade(exchange, buy, event));
                }
            }
        }

        // 최신 순 정렬 후 limit 적용
        return matched.stream()
                .sorted(Comparator.comparing(MatchedTrade::sellAt).reversed())
                .limit(Math.min(limit, MAX_MATCHED))
                .toList();
    }

    private MatchedTrade buildTrade(ExchangeMode exchange, TradingFlowHistory buy, TradingFlowHistory sell) {
        BigDecimal buyPrice = buy.currentPrice() != null ? buy.currentPrice() : BigDecimal.ZERO;
        BigDecimal sellPrice = sell.currentPrice() != null ? sell.currentPrice() : BigDecimal.ZERO;
        long holdingSeconds = ChronoUnit.SECONDS.between(buy.createdAt(), sell.createdAt());
        BigDecimal profitRate = buyPrice.compareTo(BigDecimal.ZERO) > 0
                ? sellPrice.subtract(buyPrice)
                .divide(buyPrice, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        String exitReason = classifyExitReason(sell);

        return new MatchedTrade(exchange, buy.market(), buy.createdAt(), buyPrice,
                sell.createdAt(), sellPrice, holdingSeconds, profitRate, exitReason);
    }

    private String classifyExitReason(TradingFlowHistory sell) {
        String reason = sell.signalReason() != null ? sell.signalReason() : "";
        String message = sell.message() != null ? sell.message() : "";
        if (reason.contains("Take profit") || message.contains("Take profit")) {
            return "TAKE_PROFIT";
        }
        if (reason.contains("Stop loss") || message.contains("Stop loss")) {
            return "STOP_LOSS";
        }
        if (reason.contains("Trailing") || message.contains("Trailing")) {
            return "TRAILING_STOP";
        }
        return "MANUAL";
    }
}
