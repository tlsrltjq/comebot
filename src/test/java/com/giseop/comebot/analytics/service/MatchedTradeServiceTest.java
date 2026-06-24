package com.giseop.comebot.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.analytics.dto.MatchedTrade;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.portfolio.domain.PaperTradeLog;
import com.giseop.comebot.portfolio.repository.InMemoryPaperPortfolioRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchedTradeServiceTest {

    private final InMemoryPaperPortfolioRepository repository = new InMemoryPaperPortfolioRepository();
    private final MatchedTradeService service = new MatchedTradeService(repository);

    @Test
    void matchedTradeUsesRealizedProfitInsteadOfNaiveBuySellPriceDifference() {
        Instant buyAt = Instant.parse("2026-06-23T06:42:01Z");
        Instant sellAt = Instant.parse("2026-06-23T07:09:30Z");
        repository.saveTradeLog(ExchangeMode.BINANCE, new PaperTradeLog(
                "DEXEUSDT",
                OrderSide.BUY,
                new BigDecimal("0.45281651"),
                new BigDecimal("22.084"),
                new BigDecimal("10.00000000"),
                null,
                buyAt
        ));
        repository.saveTradeLog(ExchangeMode.BINANCE, new PaperTradeLog(
                "DEXEUSDT",
                OrderSide.SELL,
                new BigDecimal("0.41329145"),
                new BigDecimal("23.663"),
                new BigDecimal("9.77971565715"),
                new BigDecimal("-0.22028434285"),
                sellAt
        ));

        List<MatchedTrade> trades = service.findRecent(ExchangeMode.BINANCE, 10);

        assertThat(trades).hasSize(1);
        assertThat(trades.getFirst().profitRatePct()).isEqualByComparingTo("-2.2028");
        assertThat(trades.getFirst().exitReason()).isEqualTo("STOP_LOSS");
    }

    @Test
    void positiveRealizedProfitIsTakeProfit() {
        Instant buyAt = Instant.parse("2026-06-23T11:30:23Z");
        Instant sellAt = Instant.parse("2026-06-23T16:23:54Z");
        repository.saveTradeLog(ExchangeMode.BINANCE, new PaperTradeLog(
                "XPLUSDT",
                OrderSide.BUY,
                new BigDecimal("115.20737327"),
                new BigDecimal("0.0868"),
                new BigDecimal("10.00000000"),
                null,
                buyAt
        ));
        repository.saveTradeLog(ExchangeMode.BINANCE, new PaperTradeLog(
                "XPLUSDT",
                OrderSide.SELL,
                new BigDecimal("115.20737327"),
                new BigDecimal("0.0903"),
                new BigDecimal("10.403225806445"),
                new BigDecimal("0.403225806445"),
                sellAt
        ));

        List<MatchedTrade> trades = service.findRecent(ExchangeMode.BINANCE, 10);

        assertThat(trades).hasSize(1);
        assertThat(trades.getFirst().profitRatePct()).isEqualByComparingTo("4.0323");
        assertThat(trades.getFirst().exitReason()).isEqualTo("TAKE_PROFIT");
    }
}
