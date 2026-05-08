package com.giseop.comebot.market.provider;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.market.domain.PriceSource;
import com.giseop.comebot.market.domain.TickerSnapshot;
import com.giseop.comebot.market.service.TickerSnapshotStore;
import com.giseop.comebot.market.websocket.MarketWebSocketProperties;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
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
        return markets.stream()
                .filter(market -> market != null && !market.isBlank())
                .map(this::normalize)
                .distinct()
                .map(this::getCurrentPrice)
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
