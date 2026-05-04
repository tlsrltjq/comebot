package com.giseop.comebot.market.candle.provider;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.giseop.comebot.market.candle.domain.Candle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class UpbitCandleProvider implements CandleProvider {

    private static final String UPBIT_API_BASE_URL = "https://api.upbit.com";
    private static final List<Integer> SUPPORTED_MINUTE_UNITS = List.of(1, 3, 5, 10, 15, 30, 60, 240);
    private static final int MIN_COUNT = 1;
    private static final int MAX_COUNT = 200;
    private static final long MIN_REQUEST_INTERVAL_MILLIS = 250;
    private static final long TOO_MANY_REQUESTS_BACKOFF_MILLIS = 1000;
    private static final int MAX_ATTEMPTS = 3;

    private final RestClient restClient;
    private final Object requestThrottleLock = new Object();
    private long lastRequestStartedAtMillis = 0;

    public UpbitCandleProvider() {
        this(RestClient.builder().baseUrl(UPBIT_API_BASE_URL).build());
    }

    UpbitCandleProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<Candle> getRecentCandles(String market, int unitMinutes, int count) {
        validateRequest(market, unitMinutes, count);
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            waitForRequestSlot();
            try {
                List<UpbitMinuteCandleResponse> response = restClient.get()
                        .uri("/v1/candles/minutes/{unit}?market={market}&count={count}", unitMinutes, market, count)
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<UpbitMinuteCandleResponse>>() {
                        });

                if (response == null || response.isEmpty()) {
                    throw new IllegalStateException("Upbit candle response is empty");
                }

                return response.stream()
                        .map(UpbitMinuteCandleResponse::toCandle)
                        .toList();
            } catch (HttpClientErrorException.TooManyRequests exception) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new IllegalStateException("Failed to fetch Upbit candle data", exception);
                }
                sleep(TOO_MANY_REQUESTS_BACKOFF_MILLIS);
            } catch (RestClientException exception) {
                throw new IllegalStateException("Failed to fetch Upbit candle data", exception);
            }
        }
        throw new IllegalStateException("Failed to fetch Upbit candle data");
    }

    private void waitForRequestSlot() {
        synchronized (requestThrottleLock) {
            long now = System.currentTimeMillis();
            long waitMillis = lastRequestStartedAtMillis + MIN_REQUEST_INTERVAL_MILLIS - now;
            if (waitMillis > 0) {
                sleep(waitMillis);
                now = System.currentTimeMillis();
            }
            lastRequestStartedAtMillis = now;
        }
    }

    private void sleep(long waitMillis) {
        try {
            Thread.sleep(waitMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to fetch Upbit candle data", exception);
        }
    }

    private void validateRequest(String market, int unitMinutes, int count) {
        if (market == null || market.isBlank()) {
            throw new IllegalArgumentException("market must not be blank");
        }
        if (!SUPPORTED_MINUTE_UNITS.contains(unitMinutes)) {
            throw new IllegalArgumentException("unsupported candle minute unit: " + unitMinutes);
        }
        if (count < MIN_COUNT || count > MAX_COUNT) {
            throw new IllegalArgumentException("count must be between 1 and 200");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UpbitMinuteCandleResponse(
            String market,
            Long timestamp,
            BigDecimal openingPrice,
            BigDecimal highPrice,
            BigDecimal lowPrice,
            BigDecimal tradePrice,
            BigDecimal candleAccTradePrice,
            BigDecimal candleAccTradeVolume
    ) {
        @JsonCreator
        UpbitMinuteCandleResponse(
                @JsonProperty("market") String market,
                @JsonProperty("timestamp") Long timestamp,
                @JsonProperty("opening_price") BigDecimal openingPrice,
                @JsonProperty("high_price") BigDecimal highPrice,
                @JsonProperty("low_price") BigDecimal lowPrice,
                @JsonProperty("trade_price") BigDecimal tradePrice,
                @JsonProperty("candle_acc_trade_price") BigDecimal candleAccTradePrice,
                @JsonProperty("candle_acc_trade_volume") BigDecimal candleAccTradeVolume
        ) {
            this.market = market;
            this.timestamp = timestamp;
            this.openingPrice = openingPrice;
            this.highPrice = highPrice;
            this.lowPrice = lowPrice;
            this.tradePrice = tradePrice;
            this.candleAccTradePrice = candleAccTradePrice;
            this.candleAccTradeVolume = candleAccTradeVolume;
        }

        private Candle toCandle() {
            if (timestamp == null) {
                throw new IllegalStateException("Upbit candle timestamp is missing");
            }
            return new Candle(
                    market,
                    Instant.ofEpochMilli(timestamp),
                    openingPrice,
                    highPrice,
                    lowPrice,
                    tradePrice,
                    candleAccTradePrice,
                    candleAccTradeVolume
            );
        }
    }
}
