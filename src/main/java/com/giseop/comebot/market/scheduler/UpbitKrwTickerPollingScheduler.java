package com.giseop.comebot.market.scheduler;

import com.giseop.comebot.market.dto.UpbitKrwTickerResponse;
import com.giseop.comebot.market.service.UpbitKrwTickerStore;
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
public class UpbitKrwTickerPollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(UpbitKrwTickerPollingScheduler.class);
    private static final String UPBIT_API_BASE_URL = "https://api.upbit.com";
    private static final String KRW_TICKER_ALL_PATH = "/v1/ticker/all?quote_currencies=KRW";
    private static final int SAMPLE_SIZE = 5;

    private final RestClient restClient;
    private final AtomicBoolean running;
    private final UpbitKrwTickerStore tickerStore;
    private final UpbitKrwTickerPollingProperties properties;

    public UpbitKrwTickerPollingScheduler() {
        this(
                RestClient.builder().baseUrl(UPBIT_API_BASE_URL).build(),
                new AtomicBoolean(false),
                new UpbitKrwTickerStore(),
                new UpbitKrwTickerPollingProperties()
        );
    }

    @Autowired
    public UpbitKrwTickerPollingScheduler(
            UpbitKrwTickerStore tickerStore,
            UpbitKrwTickerPollingProperties properties
    ) {
        this(RestClient.builder().baseUrl(UPBIT_API_BASE_URL).build(), new AtomicBoolean(false), tickerStore, properties);
    }

    UpbitKrwTickerPollingScheduler(RestClient restClient, AtomicBoolean running) {
        this(restClient, running, new UpbitKrwTickerStore(), new UpbitKrwTickerPollingProperties());
    }

    UpbitKrwTickerPollingScheduler(RestClient restClient, AtomicBoolean running, UpbitKrwTickerStore tickerStore) {
        this(restClient, running, tickerStore, new UpbitKrwTickerPollingProperties());
    }

    UpbitKrwTickerPollingScheduler(
            RestClient restClient,
            AtomicBoolean running,
            UpbitKrwTickerStore tickerStore,
            UpbitKrwTickerPollingProperties properties
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
            fixedDelayString = "${market.upbit-krw-ticker-polling.fixed-delay-ms:600000}",
            initialDelayString = "${market.upbit-krw-ticker-polling.fixed-delay-ms:600000}"
    )
    public void poll() {
        refresh("scheduled");
    }

    private void refresh(String reason) {
        if (!properties.isEnabled()) {
            log.debug("Upbit KRW market universe refresh is disabled");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            log.debug("Upbit KRW market universe refresh skipped because previous run is still active");
            return;
        }

        try {
            List<UpbitKrwTickerResponse> tickers = fetchTickers();
            tickerStore.replace(tickers);
            log.info(
                    "Upbit KRW market universe refreshed. reason={}, count={}, samples={}",
                    reason,
                    tickers.size(),
                    samples(tickers)
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "Upbit KRW market universe refresh failed. reason={}, error={}",
                    reason,
                    exception.getClass().getSimpleName(),
                    exception
            );
        } finally {
            running.set(false);
        }
    }

    private List<UpbitKrwTickerResponse> fetchTickers() {
        List<UpbitKrwTickerResponse> response = restClient.get()
                .uri(KRW_TICKER_ALL_PATH)
                .retrieve()
                .body(new ParameterizedTypeReference<List<UpbitKrwTickerResponse>>() {
                });
        return response == null ? List.of() : response;
    }

    private List<String> samples(List<UpbitKrwTickerResponse> tickers) {
        return tickers.stream()
                .limit(SAMPLE_SIZE)
                .map(ticker -> ticker.market() + "=" + ticker.tradePrice())
                .toList();
    }
}
