package com.giseop.comebot.market.service;

import com.giseop.comebot.market.candle.domain.Candle;
import com.giseop.comebot.market.candle.provider.CandleProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class BtcTrendCacheService {

    public enum BtcTrend { UP, DOWN, NEUTRAL }

    private static final Logger log = LoggerFactory.getLogger(BtcTrendCacheService.class);
    private static final String BTC_MARKET = "KRW-BTC";
    private static final int CANDLE_UNIT_MINUTES = 60;
    private static final int CANDLE_COUNT = 20;
    private static final int EMA_SHORT = 5;
    private static final int EMA_LONG = 10;
    private static final int SCALE = 8;

    private final CandleProvider candleProvider;
    private final AtomicReference<BtcTrend> cached = new AtomicReference<>(BtcTrend.NEUTRAL);

    public BtcTrendCacheService(CandleProvider candleProvider) {
        this.candleProvider = candleProvider;
    }

    @PostConstruct
    public void bootstrap() {
        refresh();
    }

    @Scheduled(fixedDelay = 300000)
    public void refresh() {
        try {
            List<Candle> candles = candleProvider.getRecentCandles(BTC_MARKET, CANDLE_UNIT_MINUTES, CANDLE_COUNT).stream()
                    .filter(c -> c != null && c.tradePrice() != null && c.tradePrice().signum() > 0)
                    .sorted(Comparator.comparing(Candle::candleTime))
                    .toList();

            if (candles.size() < EMA_LONG) {
                log.debug("BTC trend refresh skipped: not enough candles. count={}", candles.size());
                return;
            }

            List<BigDecimal> closes = candles.stream().map(Candle::tradePrice).toList();
            BigDecimal emaShort = ema(closes, EMA_SHORT);
            BigDecimal emaLong = ema(closes, EMA_LONG);

            BtcTrend trend = emaShort.compareTo(emaLong) > 0 ? BtcTrend.UP
                    : emaShort.compareTo(emaLong) < 0 ? BtcTrend.DOWN
                    : BtcTrend.NEUTRAL;
            cached.set(trend);
            log.debug("BTC 1h trend updated. trend={}, ema{}={}, ema{}={}", trend, EMA_SHORT, emaShort, EMA_LONG, emaLong);
        } catch (RuntimeException e) {
            log.warn("BTC trend refresh failed. error={}", e.getMessage());
        }
    }

    public BtcTrend trend() {
        return cached.get();
    }

    private BigDecimal ema(List<BigDecimal> prices, int period) {
        BigDecimal k = BigDecimal.valueOf(2.0).divide(BigDecimal.valueOf(period + 1), SCALE, RoundingMode.HALF_UP);
        BigDecimal oneMinusK = BigDecimal.ONE.subtract(k);
        BigDecimal result = prices.get(0);
        for (int i = 1; i < prices.size(); i++) {
            result = prices.get(i).multiply(k).add(result.multiply(oneMinusK)).setScale(SCALE, RoundingMode.HALF_UP);
        }
        return result;
    }
}
