package com.giseop.comebot.market.candle.provider;

import com.giseop.comebot.market.candle.domain.Candle;
import java.util.List;

public interface CandleProvider {

    List<Candle> getRecentCandles(String market, int unitMinutes, int count);
}
