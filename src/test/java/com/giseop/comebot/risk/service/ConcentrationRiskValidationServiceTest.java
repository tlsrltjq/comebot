package com.giseop.comebot.risk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.portfolio.domain.PaperPortfolio;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.risk.ConcentrationRiskProperties;
import com.giseop.comebot.risk.domain.RiskDecision;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConcentrationRiskValidationServiceTest {

    @Test
    void disabledConcentrationRiskApprovesBuy() {
        ConcentrationRiskProperties properties = new ConcentrationRiskProperties();
        properties.setEnabled(false);

        var result = service(properties, portfolio("900000", position("KRW-BTC", "90000")))
                .validate(ExchangeMode.UPBIT, buy("KRW-BTC", "1", "20000"));

        assertThat(result.decision()).isEqualTo(RiskDecision.APPROVED);
    }

    @Test
    void rejectsUpbitBuyWhenNextExposureReachesBlockRate() {
        ConcentrationRiskProperties properties = new ConcentrationRiskProperties();
        properties.setEnabled(true);

        var result = service(properties, portfolio("910000", position("KRW-BTC", "90000")))
                .validate(ExchangeMode.UPBIT, buy("KRW-BTC", "1", "10000"));

        assertThat(result.decision()).isEqualTo(RiskDecision.REJECTED);
        assertThat(result.reason()).contains("Market concentration exceeds block exposure rate", "KRW-BTC", "10%");
    }

    @Test
    void approvesUpbitBuyBelowBlockRate() {
        ConcentrationRiskProperties properties = new ConcentrationRiskProperties();
        properties.setEnabled(true);

        var result = service(properties, portfolio("930000", position("KRW-BTC", "60000")))
                .validate(ExchangeMode.UPBIT, buy("KRW-BTC", "1", "10000"));

        assertThat(result.decision()).isEqualTo(RiskDecision.APPROVED);
    }

    @Test
    void rejectsBinanceBuyAtSeparateBlockRate() {
        ConcentrationRiskProperties properties = new ConcentrationRiskProperties();
        properties.setEnabled(true);

        var result = service(properties, portfolio(ExchangeMode.BINANCE, "600", position("UTKUSDT", "350")))
                .validate(ExchangeMode.BINANCE, buy("UTKUSDT", "1", "50"));

        assertThat(result.decision()).isEqualTo(RiskDecision.REJECTED);
        assertThat(result.reason()).contains("UTKUSDT", "40%");
    }

    @Test
    void sellOrdersAreNotBlockedByConcentrationRisk() {
        ConcentrationRiskProperties properties = new ConcentrationRiskProperties();
        properties.setEnabled(true);

        var result = service(properties, portfolio("10000", position("KRW-BTC", "990000")))
                .validate(ExchangeMode.UPBIT, new OrderRequest(
                        "KRW-BTC",
                        OrderSide.SELL,
                        new BigDecimal("1"),
                        new BigDecimal("10000"),
                        Instant.now()
                ));

        assertThat(result.decision()).isEqualTo(RiskDecision.APPROVED);
    }

    private ConcentrationRiskValidationService service(
            ConcentrationRiskProperties properties,
            PaperPortfolioService portfolioService
    ) {
        return new ConcentrationRiskValidationService(properties, portfolioService);
    }

    private PaperPortfolioService portfolio(String cash, PaperPosition position) {
        return portfolio(ExchangeMode.UPBIT, cash, position);
    }

    private PaperPortfolioService portfolio(ExchangeMode exchange, String cash, PaperPosition position) {
        PaperPortfolioService service = mock(PaperPortfolioService.class);
        when(service.getPortfolio(exchange)).thenReturn(new PaperPortfolio(
                exchange,
                exchange == ExchangeMode.BINANCE ? "USDT" : "KRW",
                new BigDecimal(cash),
                BigDecimal.ZERO,
                List.of(position)
        ));
        return service;
    }

    private PaperPosition position(String market, String costBasis) {
        return new PaperPosition(market, BigDecimal.ONE, new BigDecimal(costBasis));
    }

    private OrderRequest buy(String market, String quantity, String price) {
        return new OrderRequest(market, OrderSide.BUY, new BigDecimal(quantity), new BigDecimal(price), Instant.now());
    }
}
