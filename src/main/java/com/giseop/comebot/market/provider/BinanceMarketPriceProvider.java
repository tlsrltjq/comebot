package com.giseop.comebot.market.provider;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.giseop.comebot.market.domain.MarketPrice;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "market.price-provider", havingValue = "BINANCE")
public class BinanceMarketPriceProvider implements MarketPriceProvider {

    private static final String BINANCE_API_BASE_URL = "https://api.binance.com";
    private static final String SUPPORTED_QUOTE = "USDT";

    private final RestClient restClient;

    public BinanceMarketPriceProvider() {
        this(RestClient.builder().baseUrl(BINANCE_API_BASE_URL).build());
    }

    BinanceMarketPriceProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public MarketPrice getCurrentPrice(String market) {
        String symbol = normalizeSymbol(market);
        try {
            BinanceTickerPriceResponse response = restClient.get()
                    .uri("/api/v3/ticker/price?symbol={symbol}", symbol)
                    .retrieve()
                    .body(BinanceTickerPriceResponse.class);

            if (response == null || response.price() == null) {
                throw new IllegalStateException("Binance ticker price response is empty");
            }

            String responseSymbol = response.symbol() == null || response.symbol().isBlank()
                    ? symbol
                    : response.symbol();
            return new MarketPrice(responseSymbol, response.price(), Instant.now());
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to fetch Binance ticker price", exception);
        }
    }

    @Override
    public List<MarketPrice> getCurrentPrices(List<String> markets) {
        if (markets == null) {
            return List.of();
        }
        return markets.stream()
                .filter(market -> market != null && !market.isBlank())
                .map(this::normalizeSymbol)
                .distinct()
                .map(this::getCurrentPrice)
                .toList();
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BinanceTickerPriceResponse(
            String symbol,
            BigDecimal price
    ) {
        @JsonCreator
        BinanceTickerPriceResponse(
                @JsonProperty("symbol") String symbol,
                @JsonProperty("price") BigDecimal price
        ) {
            this.symbol = symbol;
            this.price = price;
        }
    }
}
