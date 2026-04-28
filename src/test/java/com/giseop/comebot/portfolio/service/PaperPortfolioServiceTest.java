package com.giseop.comebot.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;

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

    private OrderResult result(OrderSide side, String quantity, String price) {
        return new OrderResult(
                "KRW-BTC",
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
