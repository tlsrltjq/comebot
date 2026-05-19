package com.giseop.comebot.market.candle.provider;

import com.giseop.comebot.market.candle.domain.Candle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class BinanceCandleProvider implements CandleProvider {

    private static final String BINANCE_API_BASE_URL = "https://api.binance.com";
    private static final String SUPPORTED_QUOTE = "USDT";
    private static final int MIN_COUNT = 1;
    private static final int MAX_COUNT = 1000;
    private static final Map<Integer, String> INTERVALS_BY_MINUTE_UNIT = Map.of(
            1, "1m",
            3, "3m",
            5, "5m",
            15, "15m",
            30, "30m",
            60, "1h",
            240, "4h"
    );

    private final RestClient restClient;

    public BinanceCandleProvider() {
        this(RestClient.builder().baseUrl(BINANCE_API_BASE_URL).build());
    }

    BinanceCandleProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<Candle> getRecentCandles(String market, int unitMinutes, int count) {
        String symbol = normalizeSymbol(market);
        String interval = interval(unitMinutes);
        validateCount(count);

        try {
            List<List<Object>> response = restClient.get()
                    .uri("/api/v3/klines?symbol={symbol}&interval={interval}&limit={count}", symbol, interval, count)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<List<Object>>>() {
                    });

            if (response == null || response.isEmpty()) {
                throw new IllegalStateException("Binance kline response is empty");
            }

            return response.stream()
                    .map(row -> toCandle(symbol, row))
                    .toList();
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to fetch Binance candle data", exception);
        }
    }

    private Candle toCandle(String symbol, List<Object> row) {
        if (row == null || row.size() < 8) {
            throw new IllegalStateException("Binance kline row is incomplete");
        }
        return new Candle(
                symbol,
                Instant.ofEpochMilli(requiredLong(row.get(0), "open time")),
                requiredBigDecimal(row.get(1), "open price"),
                requiredBigDecimal(row.get(2), "high price"),
                requiredBigDecimal(row.get(3), "low price"),
                requiredBigDecimal(row.get(4), "close price"),
                requiredBigDecimal(row.get(7), "quote asset volume"),
                requiredBigDecimal(row.get(5), "base asset volume")
        );
    }

    private String normalizeSymbol(String market) {
        if (market == null || market.isBlank()) {
            throw new IllegalArgumentException("market must not be blank");
        }
        String symbol = market.trim().toUpperCase();
        if (!symbol.endsWith(SUPPORTED_QUOTE)) {
            throw new IllegalArgumentException("Only Binance USDT spot symbols are supported");
        }
        if (!symbol.matches("[A-Z0-9]+")) {
            throw new IllegalArgumentException("Binance symbol must contain only uppercase letters and numbers");
        }
        return symbol;
    }

    private String interval(int unitMinutes) {
        String interval = INTERVALS_BY_MINUTE_UNIT.get(unitMinutes);
        if (interval == null) {
            throw new IllegalArgumentException("unsupported Binance candle minute unit: " + unitMinutes);
        }
        return interval;
    }

    private void validateCount(int count) {
        if (count < MIN_COUNT || count > MAX_COUNT) {
            throw new IllegalArgumentException("count must be between 1 and 1000");
        }
    }

    private long requiredLong(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("Binance kline " + fieldName + " is missing");
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private BigDecimal requiredBigDecimal(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("Binance kline " + fieldName + " is missing");
        }
        return new BigDecimal(value.toString());
    }
}
