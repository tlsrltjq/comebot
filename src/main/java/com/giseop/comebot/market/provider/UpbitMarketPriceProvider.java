package com.giseop.comebot.market.provider;

import com.giseop.comebot.market.domain.MarketPrice;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "market.price-provider", havingValue = "UPBIT")
public class UpbitMarketPriceProvider implements MarketPriceProvider {

    private static final String UPBIT_API_BASE_URL = "https://api.upbit.com";

    private final RestClient restClient;

    public UpbitMarketPriceProvider() {
        this(RestClient.builder().baseUrl(UPBIT_API_BASE_URL).build());
    }

    UpbitMarketPriceProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public MarketPrice getCurrentPrice(String market) {
        try {
            List<UpbitTickerResponse> response = restClient.get()
                    .uri("/v1/ticker?markets={market}", market)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<UpbitTickerResponse>>() {
                    });

            if (response == null || response.isEmpty() || response.getFirst().tradePrice() == null) {
                throw new IllegalStateException("Upbit ticker response is empty");
            }

            return new MarketPrice(market, response.getFirst().tradePrice(), Instant.now());
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to fetch Upbit ticker price", exception);
        }
    }

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
