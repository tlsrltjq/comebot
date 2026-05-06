package com.giseop.comebot.mvp2.exchange;

import java.util.List;

public interface ExchangeMarketDataProvider {

    Exchange exchange();

    ExchangeTicker getTicker(String symbol);

    List<ExchangeTicker> getTickers(List<String> symbols);

    List<ExchangeCandle> getRecentCandles(String symbol, int unitMinutes, int count);
}
