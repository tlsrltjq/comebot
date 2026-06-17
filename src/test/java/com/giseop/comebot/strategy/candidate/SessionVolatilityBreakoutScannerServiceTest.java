package com.giseop.comebot.strategy.candidate;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.candle.domain.Candle;
import com.giseop.comebot.market.candle.provider.CandleProvider;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SessionVolatilityBreakoutScannerServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T13:00:00Z"), ZoneOffset.UTC);

    private final SessionVolatilityBreakoutProperties properties = new SessionVolatilityBreakoutProperties();
    private final StubCandleProvider candleProvider = new StubCandleProvider();
    private final SessionVolatilityBreakoutScannerService service =
            new SessionVolatilityBreakoutScannerService(properties, candleProvider, CLOCK);

    @Test
    void selectsBinanceBreakoutInsideUtcSession() {
        candleProvider.candles = breakoutCandles("BTCUSDT", Instant.parse("2026-06-15T06:00:00Z"));

        TradingCandidate candidate = service.scan(ExchangeMode.BINANCE, "BTCUSDT");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SELECTED);
        assertThat(candidate.reason()).isEqualTo("Session volatility breakout selected: Binance 15m UTC06-12 close-limit");
        assertThat(candidate.currentPrice()).isEqualByComparingTo("109");
        assertThat(candidate.priceChangeRate()).isEqualByComparingTo("7.9208");
        assertThat(candidate.highLowRangeRate()).isEqualByComparingTo("11.00000000");
        assertThat(candidate.tradeAmountChangeRate()).isEqualByComparingTo("200.0000");
        assertThat(candleProvider.requestedUnitMinutes).containsExactly(15);
        assertThat(candleProvider.requestedCounts).containsExactly(70);
    }

    @Test
    void skipsOutsideUtcSession() {
        candleProvider.candles = breakoutCandles("BTCUSDT", Instant.parse("2026-06-15T12:00:00Z"));

        TradingCandidate candidate = service.scan(ExchangeMode.BINANCE, "BTCUSDT");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Outside UTC session window");
    }

    @Test
    void skipsConfiguredPeggedMarket() {
        TradingCandidate candidate = service.scan(ExchangeMode.BINANCE, "USDCUSDT");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Market is excluded from session volatility universe");
        assertThat(candleProvider.requestedMarkets).isEmpty();
    }

    @Test
    void skipsFdusdPeggedMarket() {
        TradingCandidate candidate = service.scan(ExchangeMode.BINANCE, "FDUSDUSDT");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Market is excluded from session volatility universe");
        assertThat(candleProvider.requestedMarkets).isEmpty();
    }

    @Test
    void skipsNonBinanceExchange() {
        TradingCandidate candidate = service.scan(ExchangeMode.UPBIT, "KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Session volatility breakout is Binance only");
        assertThat(candleProvider.requestedMarkets).isEmpty();
    }

    @Test
    void skipsWhenCloseDoesNotBreakPriorHigh() {
        candleProvider.candles = breakoutCandles("BTCUSDT", Instant.parse("2026-06-15T06:00:00Z"), "100");

        TradingCandidate candidate = service.scan(ExchangeMode.BINANCE, "BTCUSDT");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Close did not break prior high");
    }

    private List<Candle> breakoutCandles(String market, Instant currentOpenTime) {
        return breakoutCandles(market, currentOpenTime, "109");
    }

    private List<Candle> breakoutCandles(String market, Instant currentOpenTime, String currentClose) {
        List<Candle> candles = new ArrayList<>();
        Instant firstOpenTime = currentOpenTime.minusSeconds(60L * 15L * 60L);
        for (int index = 0; index < 60; index++) {
            candles.add(candle(market, firstOpenTime.plusSeconds(index * 15L * 60L),
                    "100", "101", "99", "100", "1000"));
        }
        candles.add(candle(market, currentOpenTime, "100", "110", "99", currentClose, "2000"));
        return candles;
    }

    private Candle candle(
            String market,
            Instant time,
            String open,
            String high,
            String low,
            String trade,
            String tradeAmount
    ) {
        return new Candle(
                market,
                time,
                new BigDecimal(open),
                new BigDecimal(high),
                new BigDecimal(low),
                new BigDecimal(trade),
                new BigDecimal(tradeAmount),
                BigDecimal.ONE
        );
    }

    private static class StubCandleProvider implements CandleProvider {

        private List<Candle> candles = List.of();
        private final List<String> requestedMarkets = new ArrayList<>();
        private final List<Integer> requestedUnitMinutes = new ArrayList<>();
        private final List<Integer> requestedCounts = new ArrayList<>();

        @Override
        public List<Candle> getRecentCandles(String market, int unitMinutes, int count) {
            requestedMarkets.add(market);
            requestedUnitMinutes.add(unitMinutes);
            requestedCounts.add(count);
            return candles;
        }
    }
}
