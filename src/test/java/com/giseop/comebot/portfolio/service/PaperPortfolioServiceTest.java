package com.giseop.comebot.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.portfolio.PaperPortfolioProperties;
import com.giseop.comebot.portfolio.repository.InMemoryPaperPortfolioRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaperPortfolioServiceTest {

    private PaperPortfolioService service;

    @BeforeEach
    void setUp() {
        PaperPortfolioProperties properties = new PaperPortfolioProperties();
        properties.setInitialCash(new BigDecimal("1000000"));
        service = new PaperPortfolioService(new InMemoryPaperPortfolioRepository(), properties);
        service.initialize();
    }

    @Test
    void buyFilledDecreasesCashAndIncreasesQuantity() {
        service.apply(result(OrderSide.BUY, "1", "100"));

        assertThat(service.getPortfolio().cash()).isEqualByComparingTo("999900");
        assertThat(service.findPositions()).hasSize(1);
        assertThat(service.findPositions().getFirst().quantity()).isEqualByComparingTo("1");
        assertThat(service.findPositions().getFirst().averageBuyPrice()).isEqualByComparingTo("100");
    }

    @Test
    void multipleBuysUpdateAverageBuyPrice() {
        service.apply(result(OrderSide.BUY, "1", "100"));
        service.apply(result(OrderSide.BUY, "1", "200"));

        assertThat(service.findPositions().getFirst().quantity()).isEqualByComparingTo("2");
        assertThat(service.findPositions().getFirst().averageBuyPrice()).isEqualByComparingTo("150");
    }

    @Test
    void sellFilledDecreasesQuantity() {
        service.apply(result(OrderSide.BUY, "2", "100"));

        service.apply(result(OrderSide.SELL, "1", "120"));

        assertThat(service.findPositions().getFirst().quantity()).isEqualByComparingTo("1");
    }

    @Test
    void sellFilledCalculatesRealizedProfit() {
        service.apply(result(OrderSide.BUY, "2", "100"));

        service.apply(result(OrderSide.SELL, "1", "120"));

        assertThat(service.getPortfolio().realizedProfit()).isEqualByComparingTo("20");
    }

    @Test
    void realizedLossSinceReturnsTodayLossOnly() {
        Instant today = Instant.parse("2026-04-29T00:00:00Z");
        service.apply(new OrderResult("KRW-BTC", OrderSide.BUY, new BigDecimal("2"), new BigDecimal("100"),
                OrderStatus.FILLED, "filled", today));

        service.apply(new OrderResult("KRW-BTC", OrderSide.SELL, new BigDecimal("1"), new BigDecimal("80"),
                OrderStatus.FILLED, "filled", today));

        assertThat(service.realizedLossSince(today)).isEqualByComparingTo("20");
    }

    @Test
    void cashShortageBuyIsRejectedByValidation() {
        assertThat(service.validate(request(OrderSide.BUY, "1", "1000001")))
                .contains("Paper cash is not enough");
    }

    @Test
    void quantityShortageSellIsRejectedByValidation() {
        assertThat(service.validate(request(OrderSide.SELL, "1", "100")))
                .contains("Paper position quantity is not enough");
    }

    @Test
    void rejectedOrFailedResultsDoNotChangePortfolio() {
        service.apply(new OrderResult("KRW-BTC", OrderSide.BUY, new BigDecimal("1"), new BigDecimal("100"),
                OrderStatus.REJECTED, "rejected", Instant.now()));
        service.apply(new OrderResult("KRW-BTC", OrderSide.BUY, new BigDecimal("1"), new BigDecimal("100"),
                OrderStatus.FAILED, "failed", Instant.now()));

        assertThat(service.getPortfolio().cash()).isEqualByComparingTo("1000000");
        assertThat(service.findPositions()).isEmpty();
    }

    @Test
    void exchangePortfoliosKeepCashAndPositionsSeparated() {
        service.apply(ExchangeMode.UPBIT, result("KRW-BTC", OrderSide.BUY, "1", "100"));
        service.apply(ExchangeMode.BINANCE, result("BTCUSDT", OrderSide.BUY, "0.01", "50000"));

        assertThat(service.getPortfolio(ExchangeMode.UPBIT).currency()).isEqualTo("KRW");
        assertThat(service.getPortfolio(ExchangeMode.UPBIT).cash()).isEqualByComparingTo("999900");
        assertThat(service.findPositions(ExchangeMode.UPBIT)).extracting("market").containsExactly("KRW-BTC");

        assertThat(service.getPortfolio(ExchangeMode.BINANCE).currency()).isEqualTo("USDT");
        assertThat(service.getPortfolio(ExchangeMode.BINANCE).cash()).isEqualByComparingTo("500.00");
        assertThat(service.findPositions(ExchangeMode.BINANCE)).extracting("market").containsExactly("BTCUSDT");
    }

    @Test
    void realizedLossIsSeparatedByExchange() {
        Instant today = Instant.parse("2026-04-29T00:00:00Z");
        service.apply(ExchangeMode.UPBIT, new OrderResult("KRW-BTC", OrderSide.BUY, new BigDecimal("2"), new BigDecimal("100"),
                OrderStatus.FILLED, "filled", today));
        service.apply(ExchangeMode.UPBIT, new OrderResult("KRW-BTC", OrderSide.SELL, new BigDecimal("1"), new BigDecimal("80"),
                OrderStatus.FILLED, "filled", today));
        service.apply(ExchangeMode.BINANCE, new OrderResult("BTCUSDT", OrderSide.BUY, new BigDecimal("1"), new BigDecimal("100"),
                OrderStatus.FILLED, "filled", today));
        service.apply(ExchangeMode.BINANCE, new OrderResult("BTCUSDT", OrderSide.SELL, new BigDecimal("1"), new BigDecimal("70"),
                OrderStatus.FILLED, "filled", today));

        assertThat(service.realizedLossSince(ExchangeMode.UPBIT, today)).isEqualByComparingTo("20");
        assertThat(service.realizedLossSince(ExchangeMode.BINANCE, today)).isEqualByComparingTo("30");
    }

    private OrderResult result(OrderSide side, String quantity, String price) {
        return result("KRW-BTC", side, quantity, price);
    }

    private OrderResult result(String market, OrderSide side, String quantity, String price) {
        return new OrderResult(
                market,
                side,
                new BigDecimal(quantity),
                new BigDecimal(price),
                OrderStatus.FILLED,
                "filled",
                Instant.now()
        );
    }

    private OrderRequest request(OrderSide side, String quantity, String price) {
        return new OrderRequest("KRW-BTC", side, new BigDecimal(quantity), new BigDecimal(price), Instant.now());
    }
}
