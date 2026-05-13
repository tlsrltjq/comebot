package com.giseop.comebot.analytics.service;

import com.giseop.comebot.analytics.dto.AnalyticsLossResponse;
import com.giseop.comebot.analytics.dto.AnalyticsPnlResponse;
import com.giseop.comebot.analytics.dto.AnalyticsRange;
import com.giseop.comebot.analytics.dto.AnalyticsSummaryResponse;
import com.giseop.comebot.analytics.dto.LossTradeResponse;
import com.giseop.comebot.analytics.dto.MarketCountResponse;
import com.giseop.comebot.analytics.dto.ReasonCountResponse;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.portfolio.dto.PortfolioValuationResponse;
import com.giseop.comebot.portfolio.service.PaperPortfolioValuationService;
import com.giseop.comebot.strategy.domain.SignalType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private static final Pattern RATE_PATTERN = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s*$");

    private final TradingFlowHistoryService tradingFlowHistoryService;
    private final PaperPortfolioValuationService paperPortfolioValuationService;
    private final Clock clock;

    @Autowired
    public AnalyticsService(
            TradingFlowHistoryService tradingFlowHistoryService,
            PaperPortfolioValuationService paperPortfolioValuationService
    ) {
        this(tradingFlowHistoryService, paperPortfolioValuationService, Clock.systemUTC());
    }

    AnalyticsService(
            TradingFlowHistoryService tradingFlowHistoryService,
            PaperPortfolioValuationService paperPortfolioValuationService,
            Clock clock
    ) {
        this.tradingFlowHistoryService = tradingFlowHistoryService;
        this.paperPortfolioValuationService = paperPortfolioValuationService;
        this.clock = clock;
    }

    public AnalyticsSummaryResponse summary(AnalyticsRange range) {
        return summary(range, ExchangeMode.UPBIT);
    }

    public AnalyticsSummaryResponse summary(AnalyticsRange range, ExchangeMode exchange) {
        Instant to = Instant.now(clock);
        Instant from = to.minus(range.duration());
        List<TradingFlowHistory> histories = tradingFlowHistoryService.findSince(exchange, from);
        List<TradingFlowHistory> stopLosses = stopLosses(histories);
        List<TradingFlowHistory> takeProfits = takeProfits(histories);

        return new AnalyticsSummaryResponse(
                range.value(),
                from,
                to,
                histories.size(),
                countSignal(histories, SignalType.BUY),
                countSignal(histories, SignalType.SELL),
                countSignal(histories, SignalType.HOLD),
                countOrderStatus(histories, OrderStatus.FILLED),
                countOrderStatus(histories, OrderStatus.REJECTED),
                countOrderStatus(histories, OrderStatus.FAILED),
                stopLosses.size(),
                takeProfits.size(),
                averageRate(stopLosses),
                averageRate(takeProfits),
                topReasons(histories),
                topMarkets(histories)
        );
    }

    public AnalyticsPnlResponse pnl(AnalyticsRange range) {
        return pnl(range, ExchangeMode.UPBIT);
    }

    public AnalyticsPnlResponse pnl(AnalyticsRange range, ExchangeMode exchange) {
        Instant to = Instant.now(clock);
        Instant from = to.minus(range.duration());
        PortfolioValuationResponse valuation = paperPortfolioValuationService.valuate(exchange);
        return new AnalyticsPnlResponse(
                range.value(),
                from,
                to,
                valuation.cash(),
                valuation.totalPositionValue(),
                valuation.totalEquity(),
                valuation.realizedProfit(),
                valuation.unrealizedProfit(),
                valuation.totalProfit(),
                valuation.positions().size()
        );
    }

    public AnalyticsLossResponse losses(AnalyticsRange range) {
        return losses(range, ExchangeMode.UPBIT);
    }

    public AnalyticsLossResponse losses(AnalyticsRange range, ExchangeMode exchange) {
        Instant from = Instant.now(clock).minus(range.duration());
        List<TradingFlowHistory> stopLosses = stopLosses(tradingFlowHistoryService.findSince(exchange, from));
        List<LossTradeResponse> worstTrades = stopLosses.stream()
                .map(this::toLossTrade)
                .filter(loss -> loss.rate() != null)
                .sorted(Comparator.comparing(LossTradeResponse::rate))
                .limit(10)
                .toList();

        return new AnalyticsLossResponse(range.value(), worstTrades, topMarkets(stopLosses));
    }

    private long countSignal(List<TradingFlowHistory> histories, SignalType signalType) {
        return histories.stream()
                .filter(history -> history.signalType() == signalType)
                .count();
    }

    private long countOrderStatus(List<TradingFlowHistory> histories, OrderStatus orderStatus) {
        return histories.stream()
                .filter(history -> history.orderStatus() == orderStatus)
                .count();
    }

    private List<TradingFlowHistory> stopLosses(List<TradingFlowHistory> histories) {
        return histories.stream()
                .filter(history -> contains(history.signalReason(), "Stop loss"))
                .toList();
    }

    private List<TradingFlowHistory> takeProfits(List<TradingFlowHistory> histories) {
        return histories.stream()
                .filter(history -> contains(history.signalReason(), "Take profit"))
                .toList();
    }

    private boolean contains(String value, String expected) {
        return value != null && value.contains(expected);
    }

    private BigDecimal averageRate(List<TradingFlowHistory> histories) {
        List<BigDecimal> rates = histories.stream()
                .map(history -> rateOf(history.signalReason()))
                .filter(Objects::nonNull)
                .toList();
        if (rates.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = rates.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(rates.size()), 8, RoundingMode.HALF_UP);
    }

    private List<ReasonCountResponse> topReasons(List<TradingFlowHistory> histories) {
        return histories.stream()
                .filter(history -> history.signalType() == SignalType.HOLD)
                .map(TradingFlowHistory::signalReason)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> new ReasonCountResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<MarketCountResponse> topMarkets(List<TradingFlowHistory> histories) {
        return histories.stream()
                .map(TradingFlowHistory::market)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> new MarketCountResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private LossTradeResponse toLossTrade(TradingFlowHistory history) {
        return new LossTradeResponse(
                history.market(),
                history.currentPrice(),
                rateOf(history.signalReason()),
                history.signalReason(),
                history.createdAt()
        );
    }

    private BigDecimal rateOf(String reason) {
        if (reason == null) {
            return null;
        }
        Matcher matcher = RATE_PATTERN.matcher(reason);
        if (!matcher.find()) {
            return null;
        }
        return new BigDecimal(matcher.group(1));
    }
}
