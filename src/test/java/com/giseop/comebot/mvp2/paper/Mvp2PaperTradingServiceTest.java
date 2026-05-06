package com.giseop.comebot.mvp2.paper;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.mvp2.exchange.Exchange;
import com.giseop.comebot.mvp2.exchange.ExchangeCandle;
import com.giseop.comebot.mvp2.exchange.ExchangeMarketDataProvider;
import com.giseop.comebot.mvp2.exchange.ExchangeTicker;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import com.giseop.comebot.strategy.indicator.VolatilityIndicatorService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class Mvp2PaperTradingServiceTest {

    @Test
    void selectedBinanceCandidateBuysIntoSeparatePaperPortfolio() {
        Mvp2PaperTradingService service = service(new StubProvider(new BigDecimal("105")));

        Mvp2PaperCandidate candidate = service.scan(Exchange.BINANCE, "BTCUSDT");
        Mvp2PaperTradingResult result = service.run(Exchange.BINANCE, "BTCUSDT");
        Mvp2PaperPortfolioSnapshot portfolio = service.portfolio(Exchange.BINANCE);

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SELECTED);
        assertThat(result.side()).isEqualTo(OrderSide.BUY);
        assertThat(result.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(portfolio.cash()).isBetween(new BigDecimal("90"), new BigDecimal("90.000001"));
        assertThat(portfolio.positions()).hasSize(1);
        assertThat(portfolio.positions().getFirst().symbol()).isEqualTo("BTCUSDT");
    }

    @Test
    void heldBinancePositionSellsOnStopLoss() {
        StubProvider provider = new StubProvider(new BigDecimal("105"));
        Mvp2PaperTradingService service = service(provider);
        service.run(Exchange.BINANCE, "BTCUSDT");

        provider.price = new BigDecimal("103");
        Mvp2PaperTradingResult result = service.run(Exchange.BINANCE, "BTCUSDT");

        assertThat(result.side()).isEqualTo(OrderSide.SELL);
        assertThat(result.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(result.reason()).contains("Stop loss rate reached");
        assertThat(service.portfolio(Exchange.BINANCE).positions()).isEmpty();
        assertThat(service.history(Exchange.BINANCE, 10)).hasSize(2);
    }

    @Test
    void binanceValuationUsesCurrentTickerForUnrealizedProfit() {
        StubProvider provider = new StubProvider(new BigDecimal("105"));
        Mvp2PaperTradingService service = service(provider);
        service.run(Exchange.BINANCE, "BTCUSDT");

        provider.price = new BigDecimal("110");
        Mvp2PaperPortfolioValuation valuation = service.valuation(Exchange.BINANCE);

        assertThat(valuation.cash()).isBetween(new BigDecimal("90"), new BigDecimal("90.000001"));
        assertThat(valuation.totalPositionValue()).isGreaterThan(new BigDecimal("10"));
        assertThat(valuation.totalEquity()).isGreaterThan(new BigDecimal("100"));
        assertThat(valuation.realizedProfit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(valuation.unrealizedProfit()).isGreaterThan(BigDecimal.ZERO);
        assertThat(valuation.totalProfit()).isGreaterThan(BigDecimal.ZERO);
        assertThat(valuation.positions()).hasSize(1);
        assertThat(valuation.positions().getFirst().currentPrice()).isEqualByComparingTo("110");
        assertThat(valuation.positions().getFirst().unrealizedProfitRate()).isGreaterThan(BigDecimal.ZERO);
    }

    private Mvp2PaperTradingService service(StubProvider provider) {
        Mvp2PaperTradingProperties properties = new Mvp2PaperTradingProperties();
        properties.setInitialCash(new BigDecimal("100"));
        properties.setOrderAmount(new BigDecimal("10"));
        properties.setMinTradeAmountChangeRate(BigDecimal.ZERO);
        properties.setBinanceSymbols(List.of("BTCUSDT"));
        Mvp2PaperPortfolioService portfolioService = new Mvp2PaperPortfolioService(properties);
        return new Mvp2PaperTradingService(
                List.of(provider),
                new VolatilityIndicatorService(),
                properties,
                portfolioService,
                new Mvp2PaperHistoryService()
        );
    }

    private static class StubProvider implements ExchangeMarketDataProvider {

        private BigDecimal price;

        StubProvider(BigDecimal price) {
            this.price = price;
        }

        @Override
        public Exchange exchange() {
            return Exchange.BINANCE;
        }

        @Override
        public ExchangeTicker getTicker(String symbol) {
            return new ExchangeTicker(Exchange.BINANCE, symbol, price, Instant.now());
        }

        @Override
        public List<ExchangeTicker> getTickers(List<String> symbols) {
            return symbols.stream()
                    .map(this::getTicker)
                    .toList();
        }

        @Override
        public List<ExchangeCandle> getRecentCandles(String symbol, int unitMinutes, int count) {
            Instant now = Instant.parse("2026-05-06T00:00:00Z");
            return List.of(
                    candle(symbol, now.minusSeconds(240), "100", "101", "99", "100", "1000"),
                    candle(symbol, now.minusSeconds(180), "100", "102", "100", "101", "1100"),
                    candle(symbol, now.minusSeconds(120), "101", "103", "101", "102", "1200"),
                    candle(symbol, now.minusSeconds(60), "102", "104", "102", "103", "1300"),
                    candle(symbol, now, "103", "106", "103", price.toPlainString(), "1500")
            );
        }

        private ExchangeCandle candle(String symbol, Instant time, String open, String high, String low, String close, String tradeAmount) {
            return new ExchangeCandle(
                    Exchange.BINANCE,
                    symbol,
                    time,
                    new BigDecimal(open),
                    new BigDecimal(high),
                    new BigDecimal(low),
                    new BigDecimal(close),
                    new BigDecimal(tradeAmount),
                    new BigDecimal("10")
            );
        }
    }
}
