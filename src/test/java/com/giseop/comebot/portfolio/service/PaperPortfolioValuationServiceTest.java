package com.giseop.comebot.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.market.provider.MarketPriceProvider;
import com.giseop.comebot.portfolio.PaperPortfolioProperties;
import com.giseop.comebot.portfolio.dto.PortfolioValuationResponse;
import com.giseop.comebot.portfolio.repository.InMemoryPaperPortfolioRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaperPortfolioValuationServiceTest {

    private PaperPortfolioService portfolioService;
    private StubMarketPriceProvider marketPriceProvider;
    private PaperPortfolioValuationService valuationService;

    @BeforeEach
    void setUp() {
        PaperPortfolioProperties properties = new PaperPortfolioProperties();
        properties.setInitialCash(new BigDecimal("1000000"));
        portfolioService = new PaperPortfolioService(new InMemoryPaperPortfolioRepository(), properties);
        portfolioService.initialize();
        marketPriceProvider = new StubMarketPriceProvider();
        valuationService = new PaperPortfolioValuationService(portfolioService, marketPriceProvider);
    }

    @Test
    void valuateReturnsEmptyPositionsWhenNoPositionExists() {
        PortfolioValuationResponse response = valuationService.valuate();

        assertThat(response.cash()).isEqualByComparingTo("1000000");
        assertThat(response.totalPositionValue()).isEqualByComparingTo("0");
        assertThat(response.totalEquity()).isEqualByComparingTo("1000000");
        assertThat(response.positions()).isEmpty();
    }

    @Test
    void valuateCalculatesPositionValueAndUnrealizedProfit() {
        portfolioService.apply(filled(OrderSide.BUY, "2", "100"));
        marketPriceProvider.prices = Map.of("KRW-BTC", new BigDecimal("150"));

        PortfolioValuationResponse response = valuationService.valuate();

        assertThat(response.positions()).hasSize(1);
        assertThat(response.positions().getFirst().positionValue()).isEqualByComparingTo("300");
        assertThat(response.positions().getFirst().unrealizedProfit()).isEqualByComparingTo("100");
    }

    @Test
    void valuateCalculatesUnrealizedProfitRate() {
        portfolioService.apply(filled(OrderSide.BUY, "2", "100"));
        marketPriceProvider.prices = Map.of("KRW-BTC", new BigDecimal("150"));

        PortfolioValuationResponse response = valuationService.valuate();

        assertThat(response.positions().getFirst().unrealizedProfitRate()).isEqualByComparingTo("50.00000000");
    }

    @Test
    void valuateCalculatesTotalEquityAndTotalProfit() {
        portfolioService.apply(filled(OrderSide.BUY, "2", "100"));
        portfolioService.apply(filled(OrderSide.SELL, "1", "120"));
        marketPriceProvider.prices = Map.of("KRW-BTC", new BigDecimal("150"));

        PortfolioValuationResponse response = valuationService.valuate();

        assertThat(response.cash()).isEqualByComparingTo("999920");
        assertThat(response.totalPositionValue()).isEqualByComparingTo("150");
        assertThat(response.totalEquity()).isEqualByComparingTo("1000070");
        assertThat(response.realizedProfit()).isEqualByComparingTo("20");
        assertThat(response.unrealizedProfit()).isEqualByComparingTo("50");
        assertThat(response.totalProfit()).isEqualByComparingTo("70");
    }

    @Test
    void valuateFailsClearlyWhenCurrentPriceLookupFails() {
        portfolioService.apply(filled(OrderSide.BUY, "1", "100"));
        marketPriceProvider.fail = true;

        assertThatThrownBy(() -> valuationService.valuate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Current price lookup failed");
    }

    private OrderResult filled(OrderSide side, String quantity, String price) {
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

    private static class StubMarketPriceProvider implements MarketPriceProvider {

        private Map<String, BigDecimal> prices = Map.of();
        private boolean fail;

        @Override
        public MarketPrice getCurrentPrice(String market) {
            if (fail) {
                throw new IllegalStateException("Current price lookup failed");
            }
            return new MarketPrice(market, prices.get(market), Instant.now());
        }
    }
}
