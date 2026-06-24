package com.giseop.comebot.portfolio.service;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.service.OrderExecutionService;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.market.provider.MarketPriceProvider;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.dto.SelectedPaperSellResponse;
import com.giseop.comebot.portfolio.dto.SelectedPaperSellResultResponse;
import com.giseop.comebot.safety.KillSwitchService;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.trading.service.TradingFlowResult;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SelectedPaperSellService {

    private final PaperPortfolioService paperPortfolioService;
    private final MarketPriceProvider marketPriceProvider;
    private final OrderExecutionService orderExecutionService;
    private final TradingFlowHistoryService tradingFlowHistoryService;
    private final KillSwitchService killSwitchService;

    public SelectedPaperSellService(
            PaperPortfolioService paperPortfolioService,
            MarketPriceProvider marketPriceProvider,
            OrderExecutionService orderExecutionService,
            TradingFlowHistoryService tradingFlowHistoryService,
            KillSwitchService killSwitchService
    ) {
        this.paperPortfolioService = paperPortfolioService;
        this.marketPriceProvider = marketPriceProvider;
        this.orderExecutionService = orderExecutionService;
        this.tradingFlowHistoryService = tradingFlowHistoryService;
        this.killSwitchService = killSwitchService;
    }

    public SelectedPaperSellResponse sellSelected(ExchangeMode exchange, List<String> markets) {
        List<String> requestedMarkets = normalizeMarkets(markets);
        if (requestedMarkets.isEmpty()) {
            throw new IllegalArgumentException("markets must not be empty");
        }

        List<TradingFlowResult> results = requestedMarkets.stream()
                .map(market -> sellOne(exchange, market))
                .toList();

        long succeededCount = results.stream()
                .filter(result -> result.orderStatus() == OrderStatus.FILLED)
                .count();
        return new SelectedPaperSellResponse(
                exchange.name(),
                requestedMarkets.size(),
                (int) succeededCount,
                results.size() - (int) succeededCount,
                results.stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    private TradingFlowResult sellOne(ExchangeMode exchange, String market) {
        if (killSwitchService.isEnabled()) {
            return save(exchange, rejected(
                    market,
                    null,
                    "Kill switch enabled",
                    "Kill switch enabled: selected PAPER sell blocked",
                    Instant.now()
            ));
        }

        PaperPosition position = paperPortfolioService.findPosition(exchange, market)
                .filter(candidate -> candidate.quantity().signum() > 0)
                .orElse(null);
        if (position == null) {
            return save(exchange, rejected(
                    market,
                    null,
                    "Position not found",
                    "Position not found",
                    Instant.now()
            ));
        }

        try {
            MarketPrice marketPrice = marketPriceProvider.getCurrentPrice(market);
            if (marketPrice == null || marketPrice.currentPrice() == null) {
                return save(exchange, failed(market, null, "Current price is not available", Instant.now()));
            }

            OrderRequest request = new OrderRequest(
                    market,
                    OrderSide.SELL,
                    position.quantity(),
                    marketPrice.currentPrice(),
                    Instant.now()
            );
            OrderResult orderResult = orderExecutionService.executePaperPositionExit(exchange, request);
            return save(exchange, new TradingFlowResult(
                    market,
                    marketPrice.currentPrice(),
                    SignalType.SELL,
                    "Selected PAPER position sell",
                    true,
                    orderResult.status(),
                    orderResult.status() == OrderStatus.FILLED ? "Selected PAPER position sold" : orderResult.message(),
                    orderResult.executedAt()
            ));
        } catch (RuntimeException exception) {
            return save(exchange, failed(market, null, failureMessage(exception), Instant.now()));
        }
    }

    private List<String> normalizeMarkets(List<String> markets) {
        if (markets == null) {
            return List.of();
        }
        return markets.stream()
                .filter(market -> market != null && !market.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private TradingFlowResult rejected(String market, java.math.BigDecimal currentPrice, String reason, String message, Instant executedAt) {
        return new TradingFlowResult(
                market,
                currentPrice,
                SignalType.SELL,
                reason,
                false,
                OrderStatus.REJECTED,
                message,
                executedAt
        );
    }

    private TradingFlowResult failed(String market, java.math.BigDecimal currentPrice, String message, Instant executedAt) {
        return new TradingFlowResult(
                market,
                currentPrice,
                SignalType.SELL,
                "Selected PAPER sell failed",
                false,
                OrderStatus.FAILED,
                message,
                executedAt
        );
    }

    private TradingFlowResult save(ExchangeMode exchange, TradingFlowResult result) {
        tradingFlowHistoryService.save(exchange, result);
        return result;
    }

    private SelectedPaperSellResultResponse toResponse(TradingFlowResult result) {
        return new SelectedPaperSellResultResponse(
                result.market(),
                result.signalType(),
                result.orderCreated(),
                result.orderStatus(),
                result.message(),
                result.executedAt()
        );
    }

    private String failureMessage(RuntimeException exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }
}
