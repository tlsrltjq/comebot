package com.giseop.comebot.backtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BacktestDecisionPolicyTest {

    @Test
    void rejectsMissingTrainGrossEdge() {
        BacktestEngine.Result result = result(
                trades("KRW-A", 20, 10, 1_000, -2_100, 1_000),
                trades("KRW-B", 10, 10, 1_000, -2_000, 2_000)
        );

        assertThat(BacktestDecisionPolicy.decide(result)).isEqualTo("reject:no-train-gross-edge");
    }

    @Test
    void rejectsOosCollapseAfterTrainEdge() {
        BacktestEngine.Result result = result(
                trades("KRW-A", 20, 10, 2_000, -1_000, 1_000),
                trades("KRW-B", 10, 10, 1_000, -2_000, 2_000)
        );

        assertThat(BacktestDecisionPolicy.decide(result)).isEqualTo("reject:oos-collapse");
    }

    @Test
    void rejectsSingleMarketConcentrationBeforeOosDecision() {
        BacktestEngine.Result result = result(
                trades("KRW-A", 25, 5, 2_000, -1_000, 1_000),
                trades("KRW-A", 10, 10, 2_000, -1_000, 2_000)
        );

        assertThat(BacktestDecisionPolicy.decide(result)).isEqualTo("reject:market-concentration");
    }

    @Test
    void acceptsStrongGrossAndNetOosCandidate() {
        BacktestEngine.Result result = result(
                trades("KRW-A", 16, 4, 2_000, -1_000, 1_000),
                trades("KRW-B", 16, 4, 2_000, -1_000, 2_000)
        );

        assertThat(BacktestDecisionPolicy.decide(result)).isEqualTo("candidate:paper-observation");
    }

    @Test
    void splitPolicyUsesLastSixtyDays() {
        long end = 1_800_000_000L;

        assertThat(BacktestSplitPolicy.splitSec(end)).isEqualTo(end - 60L * 86_400L);
        assertThat(BacktestSplitPolicy.description()).contains("last 60d");
    }

    private static BacktestEngine.Result result(List<ClosedTrade> train, List<ClosedTrade> test) {
        List<ClosedTrade> all = new ArrayList<>();
        all.addAll(train);
        all.addAll(test);
        return new BacktestEngine.Result(all, all.size(), all.size(), 0, 2_000L, 1_000_000d);
    }

    private static List<ClosedTrade> trades(
            String market,
            int wins,
            int losses,
            double winPnl,
            double lossPnl,
            long entryBase
    ) {
        List<ClosedTrade> trades = new ArrayList<>();
        for (int i = 0; i < wins; i++) {
            trades.add(trade(market, entryBase + i, winPnl, "TP"));
        }
        for (int i = 0; i < losses; i++) {
            trades.add(trade(market, entryBase + wins + i, lossPnl, "SL"));
        }
        return trades;
    }

    private static ClosedTrade trade(String market, long entry, double pnl, String reason) {
        return new ClosedTrade(market, entry, entry + 60, 100, 101, pnl, pnl, pnl / 100.0, reason);
    }
}
