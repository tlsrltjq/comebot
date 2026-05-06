package com.giseop.comebot.mvp2.exchange;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BinanceExchangeMarketDataProvider implements ExchangeMarketDataProvider {

    private static final String BINANCE_API_BASE_URL = "https://api.binance.com";
    private static final int MIN_COUNT = 1;
    private static final int MAX_COUNT = 1000;
    private static final Map<Integer, String> SUPPORTED_MINUTE_INTERVALS = Map.of(
            1, "1m",
            3, "3m",
            5, "5m",
            15, "15m",
            30, "30m",
            60, "1h",
            240, "4h"
    );

    private final RestClient restClient;
    private final ExchangeSymbolNormalizer symbolNormalizer;

    @Autowired
    public BinanceExchangeMarketDataProvider(ExchangeSymbolNormalizer symbolNormalizer) {
        this(RestClient.builder().baseUrl(BINANCE_API_BASE_URL).build(), symbolNormalizer);
    }

    BinanceExchangeMarketDataProvider(RestClient restClient, ExchangeSymbolNormalizer symbolNormalizer) {
        this.restClient = restClient;
        this.symbolNormalizer = symbolNormalizer;
    }

    @Override
    public Exchange exchange() {
        return Exchange.BINANCE;
    }

    @Override
    public ExchangeTicker getTicker(String symbol) {
        String normalizedSymbol = symbolNormalizer.normalize(exchange(), symbol);
        try {
            BinanceTickerResponse response = restClient.get()
                    .uri("/api/v3/ticker/price?symbol={symbol}", normalizedSymbol)
                    .retrieve()
                    .body(BinanceTickerResponse.class);
            if (response == null || response.symbol() == null || response.price() == null) {
                throw new IllegalStateException("Binance ticker response is empty");
            }
            return toTicker(response, Instant.now());
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to fetch Binance ticker price", exception);
        }
    }

    @Override
    public List<ExchangeTicker> getTickers(List<String> symbols) {
        if (symbols == null) {
            return List.of();
        }
        List<String> normalizedSymbols = symbols.stream()
                .filter(symbol -> symbol != null && !symbol.isBlank())
                .map(symbol -> symbolNormalizer.normalize(exchange(), symbol))
                .distinct()
                .toList();
        if (normalizedSymbols.isEmpty()) {
            return List.of();
        }
        if (normalizedSymbols.size() == 1) {
            return List.of(getTicker(normalizedSymbols.getFirst()));
        }

        try {
            List<BinanceTickerResponse> response = restClient.get()
                    .uri("/api/v3/ticker/price")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<BinanceTickerResponse>>() {
                    });
            if (response == null || response.isEmpty()) {
                throw new IllegalStateException("Binance ticker response is empty");
            }

            Instant capturedAt = Instant.now();
            Map<String, BinanceTickerResponse> responseBySymbol = response.stream()
                    .filter(ticker -> ticker.symbol() != null && ticker.price() != null)
                    .collect(Collectors.toMap(BinanceTickerResponse::symbol, Function.identity(), (first, second) -> first));
            return normalizedSymbols.stream()
                    .map(responseBySymbol::get)
                    .filter(ticker -> ticker != null)
                    .map(ticker -> toTicker(ticker, capturedAt))
                    .toList();
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to fetch Binance ticker price", exception);
        }
    }

    @Override
    public List<ExchangeCandle> getRecentCandles(String symbol, int unitMinutes, int count) {
        String normalizedSymbol = symbolNormalizer.normalize(exchange(), symbol);
        String interval = interval(unitMinutes);
        validateCount(count);

        try {
            List<List<Object>> response = restClient.get()
                    .uri("/api/v3/klines?symbol={symbol}&interval={interval}&limit={limit}", normalizedSymbol, interval, count)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<List<Object>>>() {
                    });
            if (response == null || response.isEmpty()) {
                throw new IllegalStateException("Binance candle response is empty");
            }
            return response.stream()
                    .map(row -> toCandle(normalizedSymbol, row))
                    .toList();
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to fetch Binance candle data", exception);
        }
    }

    private String interval(int unitMinutes) {
        String interval = SUPPORTED_MINUTE_INTERVALS.get(unitMinutes);
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

    private ExchangeTicker toTicker(BinanceTickerResponse response, Instant capturedAt) {
        return new ExchangeTicker(exchange(), response.symbol(), response.price(), capturedAt);
    }

    private ExchangeCandle toCandle(String symbol, List<Object> row) {
        if (row.size() < 8) {
            throw new IllegalStateException("Binance candle response row is incomplete");
        }
        return new ExchangeCandle(
                exchange(),
                symbol,
                Instant.ofEpochMilli(asLong(row.get(0))),
                new BigDecimal(asText(row.get(1))),
                new BigDecimal(asText(row.get(2))),
                new BigDecimal(asText(row.get(3))),
                new BigDecimal(asText(row.get(4))),
                new BigDecimal(asText(row.get(7))),
                new BigDecimal(asText(row.get(5)))
        );
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(asText(value));
    }

    private String asText(Object value) {
        if (value == null) {
            throw new IllegalStateException("Binance candle response value is missing");
        }
        return value.toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BinanceTickerResponse(
            String symbol,
            BigDecimal price
    ) {
        @JsonCreator
        BinanceTickerResponse(
                @JsonProperty("symbol") String symbol,
                @JsonProperty("price") BigDecimal price
        ) {
            this.symbol = symbol;
            this.price = price;
        }
    }
}
