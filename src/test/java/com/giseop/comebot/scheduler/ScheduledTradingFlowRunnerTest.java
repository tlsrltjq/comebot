package com.giseop.comebot.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.trading.service.TradingFlowResult;
import com.giseop.comebot.trading.service.TradingFlowService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScheduledTradingFlowRunnerTest {

    @Test
    void runScheduledDoesNotExecuteWhenDisabled() {
        TradingSchedulerProperties properties = new TradingSchedulerProperties();
        properties.setEnabled(false);
        properties.setMarkets(List.of("KRW-BTC"));
        RecordingTradingFlowService tradingFlowService = new RecordingTradingFlowService();

        new ScheduledTradingFlowRunner(properties, tradingFlowService).runScheduled();

        assertThat(tradingFlowService.executedMarkets).isEmpty();
    }

    @Test
    void runScheduledExecutesConfiguredMarketsWhenEnabled() {
        TradingSchedulerProperties properties = new TradingSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of("KRW-BTC", "KRW-ETH"));
        RecordingTradingFlowService tradingFlowService = new RecordingTradingFlowService();

        new ScheduledTradingFlowRunner(properties, tradingFlowService).runScheduled();

        assertThat(tradingFlowService.executedMarkets).containsExactly("KRW-BTC", "KRW-ETH");
    }

    @Test
    void runScheduledStoresEachMarketResultThroughTradingFlowService() {
        TradingSchedulerProperties properties = new TradingSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of("KRW-BTC", "KRW-ETH"));
        RecordingTradingFlowService tradingFlowService = new RecordingTradingFlowService();

        new ScheduledTradingFlowRunner(properties, tradingFlowService).runScheduled();

        assertThat(tradingFlowService.executedMarkets).hasSize(2);
    }

    @Test
    void runScheduledDoesNotExecuteWhenMarketsAreEmpty() {
        TradingSchedulerProperties properties = new TradingSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of());
        RecordingTradingFlowService tradingFlowService = new RecordingTradingFlowService();

        new ScheduledTradingFlowRunner(properties, tradingFlowService).runScheduled();

        assertThat(tradingFlowService.executedMarkets).isEmpty();
    }

    private static class RecordingTradingFlowService extends TradingFlowService {

        private final List<String> executedMarkets = new ArrayList<>();

        private RecordingTradingFlowService() {
            super(null, null, null, null, null);
        }

        @Override
        public TradingFlowResult run(String market) {
            executedMarkets.add(market);
            return null;
        }
    }
}
