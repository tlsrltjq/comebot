package com.giseop.comebot.history.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.history.repository.InMemoryTradingFlowHistoryRepository;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.trading.service.TradingFlowResult;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TradingFlowHistoryServiceTest {

    @Test
    void saveStoresHistoryWithUuid() {
        TradingFlowHistoryService service = new TradingFlowHistoryService(
                new InMemoryTradingFlowHistoryRepository()
        );

        TradingFlowHistory history = service.save(new TradingFlowResult(
                "KRW-BTC",
                new BigDecimal("100"),
                SignalType.BUY,
                "Test threshold buy signal",
                true,
                OrderStatus.FILLED,
                "Paper trading order filled",
                Instant.now()
        ));

        assertThat(history.id()).isNotBlank();
        assertThat(service.findById(history.id())).contains(history);
        assertThat(service.findRecent(20)).containsExactly(history);
    }

    @Test
    void findRecentByMarketReturnsOnlyMatchingMarketInRecentOrder() {
        TradingFlowHistoryService service = new TradingFlowHistoryService(
                new InMemoryTradingFlowHistoryRepository()
        );

        TradingFlowHistory btcOld = service.save(result("KRW-BTC"));
        TradingFlowHistory eth = service.save(result("KRW-ETH"));
        TradingFlowHistory btcNew = service.save(result("KRW-BTC"));

        assertThat(service.findRecent("KRW-BTC", 20)).containsExactly(btcNew, btcOld);
        assertThat(service.findRecent("KRW-ETH", 20)).containsExactly(eth);
        assertThat(service.findRecent("KRW-XRP", 20)).isEmpty();
    }

    private TradingFlowResult result(String market) {
        return new TradingFlowResult(
                market,
                new BigDecimal("100"),
                SignalType.BUY,
                "Test threshold buy signal",
                true,
                OrderStatus.FILLED,
                "Paper trading order filled",
                Instant.now()
        );
    }
}
