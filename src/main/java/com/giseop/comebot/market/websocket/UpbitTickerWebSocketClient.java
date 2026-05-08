package com.giseop.comebot.market.websocket;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.domain.PriceSource;
import com.giseop.comebot.market.domain.TickerSnapshot;
import com.giseop.comebot.market.service.TickerSnapshotStore;
import com.giseop.comebot.market.service.UpbitKrwTickerStore;
import com.giseop.comebot.scheduler.CandidateSchedulerProperties;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
public class UpbitTickerWebSocketClient implements WebSocketTickerClient, SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(UpbitTickerWebSocketClient.class);
    private static final URI UPBIT_WEBSOCKET_URI = URI.create("wss://api.upbit.com/websocket/v1");

    private final MarketWebSocketProperties properties;
    private final TickerSnapshotStore tickerSnapshotStore;
    private final UpbitKrwTickerStore upbitKrwTickerStore;
    private final TradingSchedulerProperties tradingSchedulerProperties;
    private final CandidateSchedulerProperties candidateSchedulerProperties;
    private final HttpClient httpClient;

    private volatile boolean running;
    private volatile WebSocket webSocket;

    public UpbitTickerWebSocketClient(
            MarketWebSocketProperties properties,
            TickerSnapshotStore tickerSnapshotStore,
            UpbitKrwTickerStore upbitKrwTickerStore,
            TradingSchedulerProperties tradingSchedulerProperties,
            CandidateSchedulerProperties candidateSchedulerProperties
    ) {
        this.properties = properties;
        this.tickerSnapshotStore = tickerSnapshotStore;
        this.upbitKrwTickerStore = upbitKrwTickerStore;
        this.tradingSchedulerProperties = tradingSchedulerProperties;
        this.candidateSchedulerProperties = candidateSchedulerProperties;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public ExchangeMode exchange() {
        return ExchangeMode.UPBIT;
    }

    @Override
    public void start() {
        if (!properties.isEnabled() || !properties.isUpbitEnabled()) {
            return;
        }
        List<String> markets = subscribedMarkets();
        if (markets.isEmpty()) {
            log.info("Upbit ticker WebSocket is enabled but no KRW markets are configured");
            return;
        }
        running = true;
        httpClient.newWebSocketBuilder()
                .buildAsync(UPBIT_WEBSOCKET_URI, new Listener())
                .thenAccept(socket -> {
                    webSocket = socket;
                    socket.sendText(subscriptionMessage(markets), true);
                    log.info("Upbit ticker WebSocket subscribed markets={}", markets.size());
                })
                .exceptionally(exception -> {
                    log.warn("Failed to connect Upbit ticker WebSocket", exception);
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

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void startAfterMarketUniverseBootstrap() {
        if (!running) {
            start();
        }
    }

    List<String> subscribedMarkets() {
        Set<String> markets = new LinkedHashSet<>();
        addConfiguredMarkets(markets, tradingSchedulerProperties.getMarkets());
        addConfiguredMarkets(markets, candidateSchedulerProperties.getMarkets());
        return List.copyOf(markets);
    }

    private void addConfiguredMarkets(Set<String> target, List<String> configuredMarkets) {
        if (configuredMarkets == null) {
            return;
        }
        if (configuredMarkets.stream().anyMatch("ALL_KRW"::equalsIgnoreCase)) {
            target.addAll(upbitKrwTickerStore.topMarkets(50));
            return;
        }
        configuredMarkets.stream()
                .filter(market -> market != null && market.startsWith("KRW-"))
                .forEach(target::add);
    }

    void handleMessage(String payload) {
        try {
            String market = TickerWebSocketJsonFields.stringField(payload, "code").orElse("");
            BigDecimal tradePrice = TickerWebSocketJsonFields.decimalField(payload, "trade_price").orElse(null);
            BigDecimal accTradePrice24h = TickerWebSocketJsonFields.decimalField(payload, "acc_trade_price_24h")
                    .orElse(null);
            if (market == null || market.isBlank() || tradePrice == null || tradePrice.signum() <= 0) {
                return;
            }
            tickerSnapshotStore.save(new TickerSnapshot(
                    ExchangeMode.UPBIT,
                    market,
                    tradePrice,
                    accTradePrice24h,
                    Instant.now(),
                    PriceSource.WEBSOCKET
            ));
        } catch (RuntimeException exception) {
            log.warn("Failed to parse Upbit ticker WebSocket message", exception);
        }
    }

    private String subscriptionMessage(List<String> markets) {
        String codes = markets.stream()
                .map(market -> "\"" + market + "\"")
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        return "[{\"ticket\":\"comebot\"},{\"type\":\"ticker\",\"codes\":[" + codes + "]}]";
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
        public CompletionStage<?> onBinary(WebSocket webSocket, java.nio.ByteBuffer data, boolean last) {
            handleMessage(StandardCharsets.UTF_8.decode(data).toString());
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            running = false;
            log.info("Upbit ticker WebSocket closed status={} reason={}", statusCode, reason);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            running = false;
            log.warn("Upbit ticker WebSocket error", error);
        }
    }
}
