package com.giseop.comebot.market.websocket;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.domain.PriceSource;
import com.giseop.comebot.market.domain.TickerSnapshot;
import com.giseop.comebot.market.service.TickerSnapshotStore;
import com.giseop.comebot.scheduler.CandidateSchedulerProperties;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class BinanceTickerWebSocketClient implements WebSocketTickerClient, SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(BinanceTickerWebSocketClient.class);

    private final MarketWebSocketProperties properties;
    private final TickerSnapshotStore tickerSnapshotStore;
    private final TradingSchedulerProperties tradingSchedulerProperties;
    private final CandidateSchedulerProperties candidateSchedulerProperties;
    private final HttpClient httpClient;

    private volatile boolean running;
    private volatile WebSocket webSocket;

    public BinanceTickerWebSocketClient(
            MarketWebSocketProperties properties,
            TickerSnapshotStore tickerSnapshotStore,
            TradingSchedulerProperties tradingSchedulerProperties,
            CandidateSchedulerProperties candidateSchedulerProperties
    ) {
        this.properties = properties;
        this.tickerSnapshotStore = tickerSnapshotStore;
        this.tradingSchedulerProperties = tradingSchedulerProperties;
        this.candidateSchedulerProperties = candidateSchedulerProperties;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public ExchangeMode exchange() {
        return ExchangeMode.BINANCE;
    }

    @Override
    public void start() {
        if (!properties.isEnabled() || !properties.isBinanceEnabled()) {
            return;
        }
        List<String> markets = subscribedMarkets();
        if (markets.isEmpty()) {
            log.info("Binance ticker WebSocket is enabled but no USDT symbols are configured");
            return;
        }
        running = true;
        httpClient.newWebSocketBuilder()
                .buildAsync(uri(markets), new Listener())
                .thenAccept(socket -> {
                    webSocket = socket;
                    log.info("Binance ticker WebSocket subscribed markets={}", markets.size());
                })
                .exceptionally(exception -> {
                    log.warn("Failed to connect Binance ticker WebSocket", exception);
                    running = false;
                    return null;
                });
    }

    @Override
    public void stop() {
        running = false;
        WebSocket current = webSocket;
        if (current != null) {
            current.sendClose(WebSocket.NORMAL_CLOSURE, "stop");
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    List<String> subscribedMarkets() {
        Set<String> markets = new LinkedHashSet<>();
        tradingSchedulerProperties.getMarkets().stream()
                .map(this::normalizeSymbol)
                .filter(symbol -> symbol.endsWith("USDT"))
                .forEach(markets::add);
        candidateSchedulerProperties.getMarkets().stream()
                .map(this::normalizeSymbol)
                .filter(symbol -> symbol.endsWith("USDT"))
                .forEach(markets::add);
        return List.copyOf(markets);
    }

    void handleMessage(String payload) {
        try {
            String market = TickerWebSocketJsonFields.stringField(payload, "s").orElse("");
            BigDecimal tradePrice = TickerWebSocketJsonFields.decimalField(payload, "c").orElse(null);
            BigDecimal accTradePrice24h = TickerWebSocketJsonFields.decimalField(payload, "q").orElse(null);
            if (market == null || market.isBlank() || tradePrice == null || tradePrice.signum() <= 0) {
                return;
            }
            tickerSnapshotStore.save(new TickerSnapshot(
                    ExchangeMode.BINANCE,
                    market,
                    tradePrice,
                    accTradePrice24h,
                    Instant.now(),
                    PriceSource.WEBSOCKET
            ));
        } catch (RuntimeException exception) {
            log.warn("Failed to parse Binance ticker WebSocket message", exception);
        }
    }

    private URI uri(List<String> markets) {
        String streams = markets.stream()
                .map(market -> market.toLowerCase(Locale.ROOT) + "@ticker")
                .reduce((left, right) -> left + "/" + right)
                .orElse("");
        return URI.create("wss://stream.binance.com:9443/stream?streams=" + streams);
    }

    private String normalizeSymbol(String market) {
        if (market == null || market.isBlank()) {
            return "";
        }
        String normalized = market.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("KRW-")) {
            return normalized.substring(4) + "USDT";
        }
        return normalized;
    }

    private class Listener implements WebSocket.Listener {

        private final StringBuilder partial = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                handleMessage(partial.toString());
                partial.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            running = false;
            log.info("Binance ticker WebSocket closed status={} reason={}", statusCode, reason);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            running = false;
            log.warn("Binance ticker WebSocket error", error);
        }
    }
}
