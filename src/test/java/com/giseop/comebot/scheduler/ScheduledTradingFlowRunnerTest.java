package com.giseop.comebot.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.history.repository.InMemoryTradingFlowHistoryRepository;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.notification.NotificationPolicyService;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.notification.TradingFlowNotificationService;
import com.giseop.comebot.safety.KillSwitchService;
import com.giseop.comebot.safety.SafetyProperties;
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

    @Test
    void runScheduledIsBlockedByTradingFlowKillSwitch() {
        TradingSchedulerProperties properties = new TradingSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of("KRW-BTC"));
        SafetyProperties safetyProperties = new SafetyProperties();
        safetyProperties.setKillSwitchEnabled(true);
        InMemoryTradingFlowHistoryRepository historyRepository = new InMemoryTradingFlowHistoryRepository();
        NotificationProperties notificationProperties = new NotificationProperties();
        TradingFlowService tradingFlowService = new TradingFlowService(
                null,
                null,
                null,
                null,
                new TradingFlowHistoryService(historyRepository),
                notificationProperties,
                new NotificationPolicyService(notificationProperties),
                new TradingFlowNotificationService(message -> {
                }),
                null,
                new KillSwitchService(safetyProperties)
        );

        new ScheduledTradingFlowRunner(properties, tradingFlowService).runScheduled();

        assertThat(historyRepository.findRecent(1)).hasSize(1);
        assertThat(historyRepository.findRecent(1).getFirst().orderStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(historyRepository.findRecent(1).getFirst().message()).isEqualTo("Kill switch enabled: trading flow blocked");
    }

    private static class RecordingTradingFlowService extends TradingFlowService {

        private final List<String> executedMarkets = new ArrayList<>();

        private RecordingTradingFlowService() {
            super(null, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public TradingFlowResult run(String market) {
            executedMarkets.add(market);
            return null;
        }

        @Override
        public List<TradingFlowResult> runAll(List<String> markets) {
            markets.forEach(this::run);
            return List.of();
        }
    }
}
