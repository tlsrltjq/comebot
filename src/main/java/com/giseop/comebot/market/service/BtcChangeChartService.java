package com.giseop.comebot.market.service;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.candle.domain.Candle;
import com.giseop.comebot.market.candle.provider.BinanceCandleProvider;
import com.giseop.comebot.market.candle.provider.CandleProvider;
import com.giseop.comebot.market.candle.provider.UpbitCandleProvider;
import com.giseop.comebot.market.dto.BtcChangeChartResponse;
import com.giseop.comebot.market.dto.BtcChangePointResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BtcChangeChartService {

    private static final int RATE_SCALE = 8;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final CandleProvider upbitCandleProvider;
    private final CandleProvider binanceCandleProvider;

    @Autowired
    public BtcChangeChartService(UpbitCandleProvider upbitCandleProvider) {
        this(upbitCandleProvider, new BinanceCandleProvider());
    }

    BtcChangeChartService(CandleProvider upbitCandleProvider, CandleProvider binanceCandleProvider) {
        this.upbitCandleProvider = upbitCandleProvider;
        this.binanceCandleProvider = binanceCandleProvider;
    }

    public BtcChangeChartResponse chart(ExchangeMode exchange, BtcChangeRange range) {
        String market = btcMarket(exchange);
        List<Candle> candles = candleProvider(exchange)
                .getRecentCandles(market, range.candleUnitMinutes(), range.candleCount()).stream()
                .filter(candle -> candle != null && candle.candleTime() != null && positive(candle.tradePrice()))
                .sorted(Comparator.comparing(Candle::candleTime))
                .toList();

        if (candles.size() < 2) {
            throw new IllegalStateException("BTC candle response must contain at least two candles");
        }

        BigDecimal basePrice = candles.getFirst().tradePrice();
        BigDecimal latestPrice = candles.getLast().tradePrice();
        List<BtcChangePointResponse> points = candles.stream()
                .map(candle -> new BtcChangePointResponse(
                        candle.candleTime(),
                        candle.tradePrice(),
                        changeRate(candle.tradePrice(), basePrice)
                ))
                .toList();

        return new BtcChangeChartResponse(
                exchange.name(),
                market,
                range.value(),
                basePrice,
                latestPrice,
                changeRate(latestPrice, basePrice),
                candles.stream().map(Candle::highPrice).filter(this::positive).max(BigDecimal::compareTo).orElse(latestPrice),
                candles.stream().map(Candle::lowPrice).filter(this::positive).min(BigDecimal::compareTo).orElse(latestPrice),
                points
        );
    }

    private CandleProvider candleProvider(ExchangeMode exchange) {
        if (exchange == ExchangeMode.BINANCE) {
            return binanceCandleProvider;
        }
        return upbitCandleProvider;
    }

    private String btcMarket(ExchangeMode exchange) {
        if (exchange == ExchangeMode.BINANCE) {
            return "BTCUSDT";
        }
        return "KRW-BTC";
    }

    private BigDecimal changeRate(BigDecimal price, BigDecimal basePrice) {
        return price.subtract(basePrice)
                .divide(basePrice, RATE_SCALE, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED);
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
