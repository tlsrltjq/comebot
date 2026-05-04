package com.giseop.comebot.market.service;

import com.giseop.comebot.market.dto.UpbitKrwTickerResponse;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UpbitKrwTickerStore {

    private static volatile List<UpbitKrwTickerResponse> latestTickers = List.of();

    public void replace(List<UpbitKrwTickerResponse> tickers) {
        latestTickers = tickers == null ? List.of() : List.copyOf(tickers);
    }

    public List<UpbitKrwTickerResponse> latestTickers() {
        return latestTickers;
    }

    public List<String> latestMarkets() {
        return topMarkets(Integer.MAX_VALUE);
    }

    public List<String> topMarkets(int limit) {
        return latestTickers.stream()
                .filter(ticker -> ticker.market() != null && ticker.market().startsWith("KRW-"))
                .sorted(Comparator
                        .comparing(this::tradeAmount24h, Comparator.reverseOrder())
                        .thenComparing(UpbitKrwTickerResponse::market))
                .map(UpbitKrwTickerResponse::market)
                .distinct()
                .limit(Math.max(0, limit))
                .toList();
    }

    private BigDecimal tradeAmount24h(UpbitKrwTickerResponse ticker) {
        return ticker.accTradePrice24h() == null ? BigDecimal.ZERO : ticker.accTradePrice24h();
    }
}
