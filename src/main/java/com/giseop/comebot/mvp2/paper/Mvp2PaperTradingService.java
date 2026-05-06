package com.giseop.comebot.mvp2.paper;

import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.mvp2.exchange.Exchange;
import com.giseop.comebot.mvp2.exchange.ExchangeCandle;
import com.giseop.comebot.mvp2.exchange.ExchangeMarketDataProvider;
import com.giseop.comebot.mvp2.exchange.ExchangeTicker;
import com.giseop.comebot.market.candle.domain.Candle;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import com.giseop.comebot.strategy.indicator.VolatilityIndicatorService;
import com.giseop.comebot.strategy.indicator.VolatilitySnapshot;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class Mvp2PaperTradingService {

    private static final int QUANTITY_SCALE = 8;

    private final List<ExchangeMarketDataProvider> providers;
    private final VolatilityIndicatorService volatilityIndicatorService;
    private final Mvp2PaperTradingProperties properties;
    private final Mvp2PaperPortfolioService portfolioService;
    private final Mvp2PaperHistoryService historyService;

    public Mvp2PaperTradingService(
            List<ExchangeMarketDataProvider> providers,
            VolatilityIndicatorService volatilityIndicatorService,
            Mvp2PaperTradingProperties properties,
            Mvp2PaperPortfolioService portfolioService,
            Mvp2PaperHistoryService historyService
    ) {
        this.providers = providers;
        this.volatilityIndicatorService = volatilityIndicatorService;
        this.properties = properties;
        this.portfolioService = portfolioService;
        this.historyService = historyService;
    }

    public List<Mvp2PaperCandidate> scanBinanceCandidates() {
        return properties.getBinanceSymbols().stream()
                .map(symbol -> scan(Exchange.BINANCE, symbol))
                .toList();
    }

    public Mvp2PaperCandidate scan(Exchange exchange, String symbol) {
        try {
            List<Candle> candles = provider(exchange).getRecentCandles(symbol, properties.getCandleUnitMinutes(), properties.getCandleCount())
                    .stream()
                    .map(this::toCandle)
                    .toList();
            return toCandidate(exchange, volatilityIndicatorService.calculate(candles));
        } catch (RuntimeException exception) {
            return new Mvp2PaperCandidate(
                    exchange,
                    symbol,
                    CandidateDecision.SKIPPED,
                    "Candidate scan failed: " + failureReason(exception),
                    null,
                    null,
                    null,
                    null,
                    null,
                    Instant.now()
            );
        }
    }

    public List<Mvp2PaperTradingResult> runBinanceSymbols() {
        return properties.getBinanceSymbols().stream()
                .map(symbol -> run(Exchange.BINANCE, symbol))
                .toList();
    }

    public Mvp2PaperTradingResult run(Exchange exchange, String symbol) {
        ExchangeTicker ticker = provider(exchange).getTicker(symbol);
        return portfolioService.findPosition(exchange, ticker.symbol())
                .map(position -> exitOrHold(exchange, ticker, position))
                .orElseGet(() -> buyIfCandidate(exchange, ticker.symbol()));
    }

    public Mvp2PaperPortfolioSnapshot portfolio(Exchange exchange) {
        return portfolioService.snapshot(exchange);
    }

    public Mvp2PaperPortfolioValuation valuation(Exchange exchange) {
        Mvp2PaperPortfolioSnapshot snapshot = portfolio(exchange);
        if (snapshot.positions().isEmpty()) {
            return new Mvp2PaperPortfolioValuation(
                    exchange,
                    snapshot.cash(),
                    BigDecimal.ZERO,
                    snapshot.cash(),
                    snapshot.realizedProfit(),
                    BigDecimal.ZERO,
                    snapshot.realizedProfit(),
                    List.of()
            );
        }

        List<String> symbols = snapshot.positions().stream()
                .map(Mvp2PaperPosition::symbol)
                .toList();
        Map<String, ExchangeTicker> tickers = provider(exchange).getTickers(symbols).stream()
                .collect(Collectors.toMap(ExchangeTicker::symbol, Function.identity()));
        List<Mvp2PaperPositionValuation> positions = snapshot.positions().stream()
                .map(position -> valuePosition(position, tickers))
                .toList();
        BigDecimal totalPositionValue = positions.stream()
                .map(Mvp2PaperPositionValuation::positionValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unrealizedProfit = positions.stream()
                .map(Mvp2PaperPositionValuation::unrealizedProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Mvp2PaperPortfolioValuation(
                exchange,
                snapshot.cash(),
                totalPositionValue,
                snapshot.cash().add(totalPositionValue),
                snapshot.realizedProfit(),
                unrealizedProfit,
                snapshot.realizedProfit().add(unrealizedProfit),
                positions
        );
    }

    public List<Mvp2PaperTradeHistory> history(Exchange exchange, int limit) {
        return historyService.recent(exchange, limit);
    }

    private Mvp2PaperTradingResult buyIfCandidate(Exchange exchange, String symbol) {
        Mvp2PaperCandidate candidate = scan(exchange, symbol);
        if (candidate.decision() != CandidateDecision.SELECTED) {
            return save(new Mvp2PaperTradingResult(
                    exchange,
                    candidate.symbol(),
                    null,
                    null,
                    candidate.currentPrice(),
                    null,
                    candidate.reason(),
                    "Candidate was not selected",
                    candidate.scannedAt()
            ));
        }

        BigDecimal quantity = properties.getOrderAmount().divide(candidate.currentPrice(), QUANTITY_SCALE, RoundingMode.DOWN);
        return execute(exchange, candidate.symbol(), OrderSide.BUY, quantity, candidate.currentPrice(), candidate.reason());
    }

    private Mvp2PaperTradingResult exitOrHold(Exchange exchange, ExchangeTicker ticker, Mvp2PaperPosition position) {
        BigDecimal profitRate = profitRate(ticker.tradePrice(), position.averageBuyPrice());
        if (profitRate.compareTo(properties.getTakeProfitRate()) >= 0) {
            return execute(exchange, ticker.symbol(), OrderSide.SELL, position.quantity(), ticker.tradePrice(), "Take profit rate reached: " + profitRate);
        }
        if (profitRate.compareTo(properties.getStopLossRate()) <= 0) {
            return execute(exchange, ticker.symbol(), OrderSide.SELL, position.quantity(), ticker.tradePrice(), "Stop loss rate reached: " + profitRate);
        }
        return save(new Mvp2PaperTradingResult(
                exchange,
                ticker.symbol(),
                null,
                null,
                ticker.tradePrice(),
                null,
                "Position exit condition not reached",
                "Position held",
                Instant.now()
        ));
    }

    private Mvp2PaperTradingResult execute(Exchange exchange, String symbol, OrderSide side, BigDecimal quantity, BigDecimal price, String reason) {
        OrderResult result = portfolioService.validate(exchange, side, symbol, quantity, price)
                .map(message -> new OrderResult(symbol, side, quantity, price, OrderStatus.REJECTED, message, Instant.now()))
                .orElseGet(() -> new OrderResult(symbol, side, quantity, price, OrderStatus.FILLED, "MVP2 paper order filled", Instant.now()));
        portfolioService.apply(exchange, result);
        return save(new Mvp2PaperTradingResult(exchange, symbol, side, quantity, price, result.status(), reason, result.message(), result.executedAt()));
    }

    private Mvp2PaperTradingResult save(Mvp2PaperTradingResult result) {
        historyService.save(new Mvp2PaperTradeHistory(
                result.exchange(),
                result.symbol(),
                result.side(),
                result.quantity(),
                result.price(),
                result.status(),
                result.reason(),
                result.message(),
                result.executedAt()
        ));
        return result;
    }

    private Mvp2PaperCandidate toCandidate(Exchange exchange, VolatilitySnapshot snapshot) {
        if (snapshot.trend() != MarketTrend.UP) {
            return skipped(exchange, snapshot, "Trend is not UP");
        }
        if (snapshot.priceChangeRate().compareTo(properties.getMinPriceChangeRate()) < 0) {
            return skipped(exchange, snapshot, "Price change rate is below threshold");
        }
        if (snapshot.tradeAmountChangeRate().compareTo(properties.getMinTradeAmountChangeRate()) < 0) {
            return skipped(exchange, snapshot, "Trade amount change rate is below threshold");
        }
        if (snapshot.priceChangeRate().compareTo(properties.getMaxPriceChangeRate()) > 0) {
            return skipped(exchange, snapshot, "Price change rate is overheated");
        }
        if (snapshot.highLowRangeRate().compareTo(properties.getMaxHighLowRangeRate()) > 0) {
            return skipped(exchange, snapshot, "High low range rate is overheated");
        }
        return new Mvp2PaperCandidate(exchange, snapshot.market(), CandidateDecision.SELECTED, "Volatility long candidate selected",
                snapshot.currentPrice(), snapshot.priceChangeRate(), snapshot.highLowRangeRate(), snapshot.tradeAmountChangeRate(), snapshot.trend(), Instant.now());
    }

    private Mvp2PaperCandidate skipped(Exchange exchange, VolatilitySnapshot snapshot, String reason) {
        return new Mvp2PaperCandidate(exchange, snapshot.market(), CandidateDecision.SKIPPED, reason,
                snapshot.currentPrice(), snapshot.priceChangeRate(), snapshot.highLowRangeRate(), snapshot.tradeAmountChangeRate(), snapshot.trend(), Instant.now());
    }

    private ExchangeMarketDataProvider provider(Exchange exchange) {
        return providers.stream()
                .filter(provider -> provider.exchange() == exchange)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("MVP2 exchange provider is not available: " + exchange));
    }

    private Candle toCandle(ExchangeCandle candle) {
        return new Candle(candle.symbol(), candle.candleTime(), candle.openingPrice(), candle.highPrice(), candle.lowPrice(),
                candle.tradePrice(), candle.accumulatedTradePrice(), candle.accumulatedTradeVolume());
    }

    private BigDecimal profitRate(BigDecimal currentPrice, BigDecimal averageBuyPrice) {
        return currentPrice.subtract(averageBuyPrice)
                .multiply(new BigDecimal("100"))
                .divide(averageBuyPrice, 8, RoundingMode.HALF_UP);
    }

    private Mvp2PaperPositionValuation valuePosition(Mvp2PaperPosition position, Map<String, ExchangeTicker> tickers) {
        ExchangeTicker ticker = tickers.get(position.symbol());
        if (ticker == null || ticker.tradePrice() == null) {
            throw new IllegalStateException("MVP2 current price is not available: " + position.symbol());
        }
        BigDecimal cost = position.quantity().multiply(position.averageBuyPrice());
        BigDecimal positionValue = position.quantity().multiply(ticker.tradePrice());
        BigDecimal unrealizedProfit = positionValue.subtract(cost);
        BigDecimal unrealizedProfitRate = cost.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : unrealizedProfit.multiply(new BigDecimal("100")).divide(cost, 8, RoundingMode.HALF_UP);

        return new Mvp2PaperPositionValuation(
                position.symbol(),
                position.quantity(),
                position.averageBuyPrice(),
                ticker.tradePrice(),
                positionValue,
                unrealizedProfit,
                unrealizedProfitRate
        );
    }

    private String failureReason(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getClass().getSimpleName() + " - " + exception.getMessage();
    }
}
