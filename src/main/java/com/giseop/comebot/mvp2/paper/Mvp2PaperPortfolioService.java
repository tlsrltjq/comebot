package com.giseop.comebot.mvp2.paper;

import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.mvp2.exchange.Exchange;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class Mvp2PaperPortfolioService {

    private static final int PRICE_SCALE = 8;

    private final Mvp2PaperTradingProperties properties;
    private final Map<Exchange, BigDecimal> cashByExchange = new EnumMap<>(Exchange.class);
    private final Map<Exchange, BigDecimal> realizedProfitByExchange = new EnumMap<>(Exchange.class);
    private final Map<Exchange, Map<String, Mvp2PaperPosition>> positionsByExchange = new EnumMap<>(Exchange.class);

    public Mvp2PaperPortfolioService(Mvp2PaperTradingProperties properties) {
        this.properties = properties;
    }

    public synchronized Optional<String> validate(Exchange exchange, OrderSide side, String symbol, BigDecimal quantity, BigDecimal price) {
        initialize(exchange);
        if (side == null) {
            return Optional.of("Order side must not be null");
        }
        if (symbol == null || symbol.isBlank()) {
            return Optional.of("Symbol must not be blank");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.of("Quantity must be greater than zero");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.of("Price must be greater than zero");
        }
        if (side == OrderSide.BUY && quantity.multiply(price).compareTo(cashByExchange.get(exchange)) > 0) {
            return Optional.of("MVP2 paper cash is not enough");
        }
        if (side == OrderSide.SELL) {
            BigDecimal currentQuantity = findPosition(exchange, symbol)
                    .map(Mvp2PaperPosition::quantity)
                    .orElse(BigDecimal.ZERO);
            if (quantity.compareTo(currentQuantity) > 0) {
                return Optional.of("MVP2 paper position quantity is not enough");
            }
        }
        return Optional.empty();
    }

    public synchronized void apply(Exchange exchange, OrderResult result) {
        initialize(exchange);
        if (result == null || result.status() != OrderStatus.FILLED) {
            return;
        }
        if (result.side() == OrderSide.BUY) {
            applyBuy(exchange, result);
        }
        if (result.side() == OrderSide.SELL) {
            applySell(exchange, result);
        }
    }

    public synchronized Mvp2PaperPortfolioSnapshot snapshot(Exchange exchange) {
        initialize(exchange);
        return new Mvp2PaperPortfolioSnapshot(
                exchange,
                cashByExchange.get(exchange),
                realizedProfitByExchange.get(exchange),
                new ArrayList<>(positionsByExchange.get(exchange).values())
        );
    }

    public synchronized Optional<Mvp2PaperPosition> findPosition(Exchange exchange, String symbol) {
        initialize(exchange);
        return Optional.ofNullable(positionsByExchange.get(exchange).get(symbol));
    }

    private void applyBuy(Exchange exchange, OrderResult result) {
        BigDecimal amount = result.quantity().multiply(result.price());
        Mvp2PaperPosition current = positionsByExchange.get(exchange)
                .getOrDefault(result.market(), new Mvp2PaperPosition(result.market(), BigDecimal.ZERO, BigDecimal.ZERO));
        BigDecimal totalQuantity = current.quantity().add(result.quantity());
        BigDecimal totalCost = current.quantity().multiply(current.averageBuyPrice()).add(amount);
        BigDecimal averageBuyPrice = totalCost.divide(totalQuantity, PRICE_SCALE, RoundingMode.HALF_UP);

        cashByExchange.put(exchange, cashByExchange.get(exchange).subtract(amount));
        positionsByExchange.get(exchange).put(result.market(), new Mvp2PaperPosition(result.market(), totalQuantity, averageBuyPrice));
    }

    private void applySell(Exchange exchange, OrderResult result) {
        Mvp2PaperPosition current = positionsByExchange.get(exchange)
                .getOrDefault(result.market(), new Mvp2PaperPosition(result.market(), BigDecimal.ZERO, BigDecimal.ZERO));
        BigDecimal remainingQuantity = current.quantity().subtract(result.quantity());
        BigDecimal profit = result.price().subtract(current.averageBuyPrice()).multiply(result.quantity());

        cashByExchange.put(exchange, cashByExchange.get(exchange).add(result.quantity().multiply(result.price())));
        realizedProfitByExchange.put(exchange, realizedProfitByExchange.get(exchange).add(profit));
        if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            positionsByExchange.get(exchange).remove(result.market());
            return;
        }
        positionsByExchange.get(exchange).put(result.market(), new Mvp2PaperPosition(result.market(), remainingQuantity, current.averageBuyPrice()));
    }

    private void initialize(Exchange exchange) {
        cashByExchange.putIfAbsent(exchange, properties.getInitialCash());
        realizedProfitByExchange.putIfAbsent(exchange, BigDecimal.ZERO);
        positionsByExchange.putIfAbsent(exchange, new java.util.LinkedHashMap<>());
    }
}
