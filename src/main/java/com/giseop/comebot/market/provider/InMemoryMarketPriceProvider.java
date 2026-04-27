package com.giseop.comebot.market.provider;

import com.giseop.comebot.market.domain.MarketPrice;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryMarketPriceProvider implements MarketPriceProvider {

    private final Map<String, BigDecimal> prices = new ConcurrentHashMap<>();

    public InMemoryMarketPriceProvider() {
        prices.put("KRW-BTC", new BigDecimal("90000000"));
        prices.put("KRW-ETH", new BigDecimal("5000000"));
    }

    @Override
    public MarketPrice getCurrentPrice(String market) {
        BigDecimal price = prices.get(market);
        return new MarketPrice(market, price, Instant.now());
    }

    public MarketPrice updatePrice(String market, BigDecimal price) {
        prices.put(market, price);
        return getCurrentPrice(market);
    }
}
