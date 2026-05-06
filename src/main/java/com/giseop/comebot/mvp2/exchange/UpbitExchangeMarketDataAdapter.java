package com.giseop.comebot.mvp2.exchange;

import com.giseop.comebot.market.candle.domain.Candle;
import com.giseop.comebot.market.candle.provider.CandleProvider;
import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.market.provider.MarketPriceProvider;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UpbitExchangeMarketDataAdapter implements ExchangeMarketDataProvider {

    private final MarketPriceProvider marketPriceProvider;
    private final CandleProvider candleProvider;
    private final ExchangeSymbolNormalizer symbolNormalizer;

    public UpbitExchangeMarketDataAdapter(
            MarketPriceProvider marketPriceProvider,
            CandleProvider candleProvider,
            ExchangeSymbolNormalizer symbolNormalizer
    ) {
        this.marketPriceProvider = marketPriceProvider;
        this.candleProvider = candleProvider;
        this.symbolNormalizer = symbolNormalizer;
    }

    @Override
    public Exchange exchange() {
        return Exchange.UPBIT;
    }

    @Override
    public ExchangeTicker getTicker(String symbol) {
        String normalizedSymbol = symbolNormalizer.normalize(exchange(), symbol);
        return toTicker(marketPriceProvider.getCurrentPrice(normalizedSymbol));
    }

    @Override
    public List<ExchangeTicker> getTickers(List<String> symbols) {
        if (symbols == null) {
            return List.of();
        }
        List<String> normalizedSymbols = symbols.stream()
                .filter(symbol -> symbol != null && !symbol.isBlank())
                .map(symbol -> symbolNormalizer.normalize(exchange(), symbol))
                .distinct()
                .toList();
        if (normalizedSymbols.isEmpty()) {
            return List.of();
        }
        return marketPriceProvider.getCurrentPrices(normalizedSymbols).stream()
                .map(this::toTicker)
                .toList();
    }

    @Override
    public List<ExchangeCandle> getRecentCandles(String symbol, int unitMinutes, int count) {
        String normalizedSymbol = symbolNormalizer.normalize(exchange(), symbol);
        return candleProvider.getRecentCandles(normalizedSymbol, unitMinutes, count).stream()
                .map(this::toCandle)
                .toList();
    }

    private ExchangeTicker toTicker(MarketPrice marketPrice) {
        return new ExchangeTicker(
                exchange(),
                marketPrice.market(),
                marketPrice.currentPrice(),
                marketPrice.capturedAt()
        );
    }

    private ExchangeCandle toCandle(Candle candle) {
        return new ExchangeCandle(
                exchange(),
                candle.market(),
                candle.candleTime(),
                candle.openingPrice(),
                candle.highPrice(),
                candle.lowPrice(),
                candle.tradePrice(),
                candle.accumulatedTradePrice(),
                candle.accumulatedTradeVolume()
        );
    }
}
