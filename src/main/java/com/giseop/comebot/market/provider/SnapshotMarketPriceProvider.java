package com.giseop.comebot.market.provider;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.market.domain.PriceSource;
import com.giseop.comebot.market.domain.TickerSnapshot;
import com.giseop.comebot.market.service.TickerSnapshotStore;
import com.giseop.comebot.market.websocket.MarketWebSocketProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "market.price-provider", havingValue = "SNAPSHOT")
public class SnapshotMarketPriceProvider implements MarketPriceProvider {

    private final TickerSnapshotStore tickerSnapshotStore;
    private final MarketWebSocketProperties marketWebSocketProperties;
    private final MarketPriceProvider upbitFallbackProvider;
    private final MarketPriceProvider binanceFallbackProvider;

    @Autowired
    public SnapshotMarketPriceProvider(
            TickerSnapshotStore tickerSnapshotStore,
            MarketWebSocketProperties marketWebSocketProperties
    ) {
        this(
                tickerSnapshotStore,
                marketWebSocketProperties,
                new UpbitMarketPriceProvider(),
                new BinanceMarketPriceProvider()
        );
    }

    SnapshotMarketPriceProvider(
            TickerSnapshotStore tickerSnapshotStore,
            MarketWebSocketProperties marketWebSocketProperties,
            MarketPriceProvider upbitFallbackProvider,
            MarketPriceProvider binanceFallbackProvider
    ) {
        this.tickerSnapshotStore = tickerSnapshotStore;
        this.marketWebSocketProperties = marketWebSocketProperties;
        this.upbitFallbackProvider = upbitFallbackProvider;
        this.binanceFallbackProvider = binanceFallbackProvider;
    }

    @Override
    public MarketPrice getCurrentPrice(String market) {
        String normalizedMarket = normalize(market);
        ExchangeMode exchange = resolveExchange(normalizedMarket);

        return tickerSnapshotStore.findFresh(
                        exchange,
                        normalizedMarket,
                        marketWebSocketProperties.orderStaleDuration(),
                        Instant.now()
                )
                .map(TickerSnapshot::toMarketPrice)
                .orElseGet(() -> fetchFallback(exchange, normalizedMarket));
    }

    @Override
    public List<MarketPrice> getCurrentPrices(List<String> markets) {
        if (markets == null) {
            return List.of();
        }
        List<String> requestedMarkets = markets.stream()
                .filter(market -> market != null && !market.isBlank())
                .map(this::normalize)
                .distinct()
                .toList();
        if (requestedMarkets.isEmpty()) {
            return List.of();
        }

        Instant now = Instant.now();
        Map<String, MarketPrice> prices = new LinkedHashMap<>();
        Map<ExchangeMode, List<String>> fallbackMarkets = new LinkedHashMap<>();

        for (String market : requestedMarkets) {
            ExchangeMode exchange = resolveExchange(market);
            tickerSnapshotStore.findFresh(
                            exchange,
                            market,
                            marketWebSocketProperties.orderStaleDuration(),
                            now
                    )
                    .map(TickerSnapshot::toMarketPrice)
                    .ifPresentOrElse(
                            price -> prices.put(market, price),
                            () -> fallbackMarkets.computeIfAbsent(exchange, ignored -> new ArrayList<>()).add(market)
                    );
        }

        fallbackMarkets.forEach((exchange, exchangeMarkets) -> fetchFallbackBatch(exchange, exchangeMarkets)
                .forEach(price -> prices.put(price.market(), price)));

        return requestedMarkets.stream()
                .map(prices::get)
                .filter(price -> price != null)
                .toList();
    }

    private MarketPrice fetchFallback(ExchangeMode exchange, String market) {
        try {
            MarketPrice fallbackPrice = fallbackProvider(exchange).getCurrentPrice(market);
            tickerSnapshotStore.save(new TickerSnapshot(
                    exchange,
                    fallbackPrice.market(),
                    fallbackPrice.currentPrice(),
                    null,
                    fallbackPrice.capturedAt(),
                    PriceSource.REST_FALLBACK
            ));
            return fallbackPrice;
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Fresh ticker snapshot is not available and REST fallback failed for " + market,
                    exception
            );
        }
    }

    private MarketPriceProvider fallbackProvider(ExchangeMode exchange) {
        if (exchange == ExchangeMode.BINANCE) {
            return binanceFallbackProvider;
        }
        return upbitFallbackProvider;
    }

    private List<MarketPrice> fetchFallbackBatch(ExchangeMode exchange, List<String> markets) {
        if (markets == null || markets.isEmpty()) {
            return List.of();
        }
        try {
            List<MarketPrice> fallbackPrices = fallbackProvider(exchange).getCurrentPrices(markets);
            fallbackPrices.forEach(price -> tickerSnapshotStore.save(new TickerSnapshot(
                    exchange,
                    price.market(),
                    price.currentPrice(),
                    null,
                    price.capturedAt(),
                    PriceSource.REST_FALLBACK
            )));
            return fallbackPrices;
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Fresh ticker snapshots are not available and REST fallback failed for " + markets,
                    exception
            );
        }
    }

    private ExchangeMode resolveExchange(String market) {
        if (market.startsWith("KRW-")) {
            return ExchangeMode.UPBIT;
        }
        if (market.endsWith("USDT")) {
            return ExchangeMode.BINANCE;
        }
        throw new IllegalArgumentException("Unsupported market for snapshot provider: " + market);
    }

    private String normalize(String market) {
        if (market == null || market.isBlank()) {
            throw new IllegalArgumentException("market must not be blank");
        }
        return market.trim().toUpperCase(Locale.ROOT);
    }
}
