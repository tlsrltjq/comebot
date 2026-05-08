package com.giseop.comebot.market.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.candle.domain.Candle;
import com.giseop.comebot.market.candle.provider.CandleProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BtcChangeChartServiceTest {

    private final StubCandleProvider upbitProvider = new StubCandleProvider();
    private final StubCandleProvider binanceProvider = new StubCandleProvider();
    private final BtcChangeChartService service = new BtcChangeChartService(upbitProvider, binanceProvider);

    @Test
    void chartUsesUpbitBtcMarketAndCalculatesChangeRateInAscendingTimeOrder() {
        upbitProvider.candles = List.of(
                candle("KRW-BTC", "2026-05-08T00:01:00Z", "110", "120", "105"),
                candle("KRW-BTC", "2026-05-08T00:00:00Z", "100", "115", "95")
        );

        var response = service.chart(ExchangeMode.UPBIT, BtcChangeRange.ONE_HOUR);

        assertThat(upbitProvider.requests).containsExactly("KRW-BTC:1:60");
        assertThat(response.exchange()).isEqualTo("UPBIT");
        assertThat(response.market()).isEqualTo("KRW-BTC");
        assertThat(response.basePrice()).isEqualByComparingTo("100");
        assertThat(response.latestPrice()).isEqualByComparingTo("110");
        assertThat(response.changeRate()).isEqualByComparingTo("10.00000000");
        assertThat(response.highPrice()).isEqualByComparingTo("120");
        assertThat(response.lowPrice()).isEqualByComparingTo("95");
        assertThat(response.points()).extracting("price").containsExactly(new BigDecimal("100"), new BigDecimal("110"));
    }

    @Test
    void chartUsesBinanceBtcMarket() {
        binanceProvider.candles = List.of(
                candle("BTCUSDT", "2026-05-08T00:00:00Z", "100", "101", "99"),
                candle("BTCUSDT", "2026-05-08T01:00:00Z", "90", "100", "89")
        );

        var response = service.chart(ExchangeMode.BINANCE, BtcChangeRange.THREE_DAYS);

        assertThat(binanceProvider.requests).containsExactly("BTCUSDT:60:72");
        assertThat(response.market()).isEqualTo("BTCUSDT");
        assertThat(response.changeRate()).isEqualByComparingTo("-10.00000000");
    }

    @Test
    void chartRequiresAtLeastTwoCandles() {
        upbitProvider.candles = List.of(candle("KRW-BTC", "2026-05-08T00:00:00Z", "100", "100", "100"));

        assertThatThrownBy(() -> service.chart(ExchangeMode.UPBIT, BtcChangeRange.ONE_DAY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least two candles");
    }

    @Test
    void rangeRejectsUnsupportedValue() {
        assertThatThrownBy(() -> BtcChangeRange.from("30d"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported BTC change range");
    }

    private Candle candle(String market, String time, String close, String high, String low) {
        return new Candle(
                market,
                Instant.parse(time),
                new BigDecimal(close),
                new BigDecimal(high),
                new BigDecimal(low),
                new BigDecimal(close),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    private static class StubCandleProvider implements CandleProvider {
        private List<Candle> candles = List.of();
        private final List<String> requests = new ArrayList<>();

        @Override
        public List<Candle> getRecentCandles(String market, int unitMinutes, int count) {
            requests.add(market + ":" + unitMinutes + ":" + count);
            return candles;
        }
    }
}
