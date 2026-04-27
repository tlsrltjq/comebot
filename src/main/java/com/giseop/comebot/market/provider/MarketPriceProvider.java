package com.giseop.comebot.market.provider;

import com.giseop.comebot.market.domain.MarketPrice;

public interface MarketPriceProvider {

    MarketPrice getCurrentPrice(String market);
}
