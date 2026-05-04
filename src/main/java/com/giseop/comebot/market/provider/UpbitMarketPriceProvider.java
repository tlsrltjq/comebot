package com.giseop.comebot.market.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.giseop.comebot.market.domain.MarketPrice;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "market.price-provider", havingValue = "UPBIT")
public class UpbitMarketPriceProvider implements MarketPriceProvider {

    private static final String UPBIT_API_BASE_URL = "https://api.upbit.com";
    private static final int TICKER_ALL_THRESHOLD = 100;

    private final RestClient restClient;

    public UpbitMarketPriceProvider() {
        this(RestClient.builder().baseUrl(UPBIT_API_BASE_URL).build());
    }

    UpbitMarketPriceProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public MarketPrice getCurrentPrice(String market) {
        List<MarketPrice> prices = getCurrentPrices(List.of(market));
        if (prices.isEmpty()) {
            throw new IllegalStateException("Upbit ticker response is empty");
        }
        return prices.getFirst();
    }

    @Override
    public List<MarketPrice> getCurrentPrices(List<String> markets) {
        List<String> requestedMarkets = markets == null ? List.of() : markets.stream()
                .filter(market -> market != null && !market.isBlank())
                .distinct()
                .toList();
        if (requestedMarkets.isEmpty()) {
            return List.of();
        }

        try {
            List<UpbitTickerResponse> response = fetchTickerResponse(requestedMarkets);

            if (response == null || response.isEmpty()) {
                throw new IllegalStateException("Upbit ticker response is empty");
            }

            Instant capturedAt = Instant.now();
            Map<String, UpbitTickerResponse> responseByMarket = response.stream()
                    .filter(ticker -> ticker.market() != null && ticker.tradePrice() != null)
                    .collect(Collectors.toMap(UpbitTickerResponse::market, Function.identity(), (first, second) -> first));

            return requestedMarkets.stream()
                    .map(responseByMarket::get)
                    .filter(ticker -> ticker != null)
                    .map(ticker -> new MarketPrice(ticker.market(), ticker.tradePrice(), capturedAt))
                    .toList();
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to fetch Upbit ticker price", exception);
        }
    }

    private List<UpbitTickerResponse> fetchTickerResponse(List<String> requestedMarkets) {
        if (requestedMarkets.size() > TICKER_ALL_THRESHOLD) {
            return restClient.get()
                    .uri("/v1/ticker/all?quote_currencies=KRW")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<UpbitTickerResponse>>() {
                    });
        }
        return restClient.get()
                .uri("/v1/ticker?markets=" + String.join(",", requestedMarkets))
                .retrieve()
                .body(new ParameterizedTypeReference<List<UpbitTickerResponse>>() {
                });
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UpbitTickerResponse(
            String market,
            BigDecimal tradePrice
    ) {
        @com.fasterxml.jackson.annotation.JsonCreator
        UpbitTickerResponse(
                @com.fasterxml.jackson.annotation.JsonProperty("market") String market,
                @com.fasterxml.jackson.annotation.JsonProperty("trade_price") BigDecimal tradePrice
        ) {
            this.market = market;
            this.tradePrice = tradePrice;
        }
    }
}
