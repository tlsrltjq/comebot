package com.giseop.comebot.strategy.service;

import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.strategy.domain.TradingSignal;

public interface TradingStrategy {

    TradingSignal evaluate(MarketPrice marketPrice);
}
