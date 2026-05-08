package com.giseop.comebot.market.service;

import com.giseop.comebot.market.dto.BinanceUsdtTickerResponse;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BinanceUsdtTickerStore {

    private static volatile List<BinanceUsdtTickerResponse> latestTickers = List.of();

    public void replace(List<BinanceUsdtTickerResponse> tickers) {
        latestTickers = tickers == null ? List.of() : List.copyOf(tickers);
    }

    public List<BinanceUsdtTickerResponse> latestTickers() {
        return latestTickers;
    }

    public List<String> latestSymbols() {
        return topSymbols(Integer.MAX_VALUE);
    }

    public List<String> topSymbols(int limit) {
        return latestTickers.stream()
                .filter(ticker -> ticker.symbol() != null && ticker.symbol().endsWith("USDT"))
                .sorted(Comparator
                        .comparing(this::quoteVolume, Comparator.reverseOrder())
                        .thenComparing(BinanceUsdtTickerResponse::symbol))
                .map(BinanceUsdtTickerResponse::symbol)
                .distinct()
                .limit(Math.max(0, limit))
                .toList();
    }

    private BigDecimal quoteVolume(BinanceUsdtTickerResponse ticker) {
        return ticker.quoteVolume() == null ? BigDecimal.ZERO : ticker.quoteVolume();
    }
}
