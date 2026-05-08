package com.giseop.comebot.market.scheduler;

import com.giseop.comebot.market.dto.BinanceUsdtTickerResponse;
import com.giseop.comebot.market.service.BinanceUsdtTickerStore;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BinanceUsdtTickerPollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BinanceUsdtTickerPollingScheduler.class);
    private static final String BINANCE_API_BASE_URL = "https://api.binance.com";
    private static final String USDT_TICKER_24H_PATH = "/api/v3/ticker/24hr";
    private static final int SAMPLE_SIZE = 5;

    private final RestClient restClient;
    private final AtomicBoolean running;
    private final BinanceUsdtTickerStore tickerStore;
    private final BinanceUsdtTickerPollingProperties properties;

    public BinanceUsdtTickerPollingScheduler() {
        this(
                RestClient.builder().baseUrl(BINANCE_API_BASE_URL).build(),
                new AtomicBoolean(false),
                new BinanceUsdtTickerStore(),
                new BinanceUsdtTickerPollingProperties()
        );
    }

    @Autowired
    public BinanceUsdtTickerPollingScheduler(
            BinanceUsdtTickerStore tickerStore,
            BinanceUsdtTickerPollingProperties properties
    ) {
        this(RestClient.builder().baseUrl(BINANCE_API_BASE_URL).build(), new AtomicBoolean(false), tickerStore, properties);
    }

    BinanceUsdtTickerPollingScheduler(RestClient restClient, AtomicBoolean running) {
        this(restClient, running, new BinanceUsdtTickerStore(), new BinanceUsdtTickerPollingProperties());
    }

    BinanceUsdtTickerPollingScheduler(
            RestClient restClient,
            AtomicBoolean running,
            BinanceUsdtTickerStore tickerStore,
            BinanceUsdtTickerPollingProperties properties
    ) {
        this.restClient = restClient;
        this.running = running;
        this.tickerStore = tickerStore;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void bootstrap() {
        if (!properties.isBootstrapOnStartup()) {
            return;
        }
        refresh("bootstrap");
    }

    @Scheduled(
            fixedDelayString = "${market.binance-usdt-ticker-polling.fixed-delay-ms:600000}",
            initialDelayString = "${market.binance-usdt-ticker-polling.fixed-delay-ms:600000}"
    )
    public void poll() {
        refresh("scheduled");
    }

    private void refresh(String reason) {
        if (!properties.isEnabled()) {
            log.debug("Binance USDT market universe refresh is disabled");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            log.debug("Binance USDT market universe refresh skipped because previous run is still active");
            return;
        }

        try {
            List<BinanceUsdtTickerResponse> tickers = fetchTickers();
            tickerStore.replace(tickers);
            log.info(
                    "Binance USDT market universe refreshed. reason={}, count={}, samples={}",
                    reason,
                    tickerStore.latestSymbols().size(),
                    samples(tickers)
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "Binance USDT market universe refresh failed. reason={}, error={}",
                    reason,
                    exception.getClass().getSimpleName(),
                    exception
            );
        } finally {
            running.set(false);
        }
    }

    private List<BinanceUsdtTickerResponse> fetchTickers() {
        List<BinanceUsdtTickerResponse> response = restClient.get()
                .uri(USDT_TICKER_24H_PATH)
                .retrieve()
                .body(new ParameterizedTypeReference<List<BinanceUsdtTickerResponse>>() {
                });
        return response == null ? List.of() : response;
    }

    private List<String> samples(List<BinanceUsdtTickerResponse> tickers) {
        return tickers.stream()
                .filter(ticker -> ticker.symbol() != null && ticker.symbol().endsWith("USDT"))
                .limit(SAMPLE_SIZE)
                .map(ticker -> ticker.symbol() + "=" + ticker.lastPrice())
                .toList();
    }
}
