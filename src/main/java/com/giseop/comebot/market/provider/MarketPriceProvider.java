package com.giseop.comebot.market.provider;

import com.giseop.comebot.market.domain.MarketPrice;
import java.util.List;

public interface MarketPriceProvider {

    MarketPrice getCurrentPrice(String market);

    default List<MarketPrice> getCurrentPrices(List<String> markets) {
        if (markets == null) {
            return List.of();
        }
        return markets.stream()
                .filter(market -> market != null && !market.isBlank())
                .map(this::getCurrentPrice)
                .toList();
    }
}
