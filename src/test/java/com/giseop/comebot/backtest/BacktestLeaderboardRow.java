package com.giseop.comebot.backtest;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * One comparable leaderboard record for a strategy/config run.
 *
 * <p>The row deliberately keeps both train and test numbers next to the full-sample
 * funnel stats, so weak OOS behavior is visible before anyone looks at the detail log.
 */
record BacktestLeaderboardRow(
        String strategy,
        String exchange,
        String timeframe,
        String marketSet,
        String entryModel,
        String params,
        String sampleStartUtc,
        String sampleEndUtc,
        long signals,
        long fills,
        long expiries,
        double fillRatePct,
        BacktestReport full,
        BacktestReport train,
        BacktestReport test,
        String monthlyPnl,
        String topMarket,
        double topMarketTradePct,
        String regimeSplit,
        String decision
) {

    private static final DateTimeFormatter UTC = DateTimeFormatter.ISO_INSTANT;

    static BacktestLeaderboardRow from(
            String strategy,
            String exchange,
            String timeframe,
            String marketSet,
            String entryModel,
            String params,
            BacktestEngine.Result result,
            String regimeSplit,
            String decision
    ) {
        String finalDecision = decision == null || decision.isBlank()
                ? BacktestDecisionPolicy.decide(result)
                : decision;
        List<ClosedTrade> trades = result.closed();
        return new BacktestLeaderboardRow(
                strategy,
                exchange,
                timeframe,
                marketSet,
                entryModel,
                params,
                sampleStart(trades),
                sampleEnd(trades),
                result.signals(),
                result.fills(),
                result.expiries(),
                result.fillRatePct(),
                result.full(),
                result.train(),
                result.test(),
                monthlyPnl(trades),
                topMarket(trades),
                topMarketTradePct(trades),
                regimeSplit,
                finalDecision
        );
    }

    static String csvHeader() {
        return String.join(",",
                "strategy", "exchange", "timeframe", "marketSet", "entryModel", "params",
                "sampleStartUtc", "sampleEndUtc", "signals", "fills", "expiries", "fillRatePct",
                "fullTrades", "fullPFgross", "fullPFnet", "fullWinRatePct", "fullNetPnl", "fullMddPct",
                "fullAvgHoldMin", "trainTrades", "trainPFgross", "trainPFnet", "trainMddPct",
                "testTrades", "testPFgross", "testPFnet", "testMddPct", "monthlyPnl",
                "topMarket", "topMarketTradePct", "regimeSplit", "decision");
    }

    String toCsv() {
        return String.join(",",
                csv(strategy),
                csv(exchange),
                csv(timeframe),
                csv(marketSet),
                csv(entryModel),
                csv(params),
                csv(sampleStartUtc),
                csv(sampleEndUtc),
                String.valueOf(signals),
                String.valueOf(fills),
                String.valueOf(expiries),
                fmt(fillRatePct),
                String.valueOf(full.trades()),
                fmt(full.grossProfitFactor()),
                fmt(full.profitFactor()),
                fmt(full.winRatePct()),
                fmt(full.netPnl()),
                fmt(full.maxDrawdownPct()),
                fmt(full.avgHoldMinutes()),
                String.valueOf(train.trades()),
                fmt(train.grossProfitFactor()),
                fmt(train.profitFactor()),
                fmt(train.maxDrawdownPct()),
                String.valueOf(test.trades()),
                fmt(test.grossProfitFactor()),
                fmt(test.profitFactor()),
                fmt(test.maxDrawdownPct()),
                csv(monthlyPnl),
                csv(topMarket),
                fmt(topMarketTradePct),
                csv(regimeSplit),
                csv(decision)
        );
    }

    static String markdownHeader() {
        return "| strategy | exchange | tf | markets | entry | full PFg/PFn | train PFg/PFn | test PFg/PFn "
                + "| trades | MDD | top market | decision |\n"
                + "|---|---|---:|---|---|---:|---:|---:|---:|---:|---|---|";
    }

    String toMarkdownRow() {
        return String.format(Locale.US,
                "| %s | %s | %s | %s | %s | %.3f/%.3f | %.3f/%.3f | %.3f/%.3f | %d | %.1f%% | %s %.1f%% | %s |",
                md(strategy),
                md(exchange),
                md(timeframe),
                md(marketSet),
                md(entryModel),
                full.grossProfitFactor(),
                full.profitFactor(),
                train.grossProfitFactor(),
                train.profitFactor(),
                test.grossProfitFactor(),
                test.profitFactor(),
                full.trades(),
                full.maxDrawdownPct(),
                md(topMarket),
                topMarketTradePct,
                md(decision));
    }

    private static String sampleStart(List<ClosedTrade> trades) {
        return trades.stream()
                .map(ClosedTrade::entryTimeSec)
                .min(Long::compareTo)
                .map(BacktestLeaderboardRow::instant)
                .orElse("");
    }

    private static String sampleEnd(List<ClosedTrade> trades) {
        return trades.stream()
                .map(ClosedTrade::exitTimeSec)
                .max(Long::compareTo)
                .map(BacktestLeaderboardRow::instant)
                .orElse("");
    }

    private static String instant(long epochSec) {
        return UTC.format(Instant.ofEpochSecond(epochSec));
    }

    private static String monthlyPnl(List<ClosedTrade> trades) {
        Map<YearMonth, Double> pnlByMonth = new TreeMap<>();
        for (ClosedTrade trade : trades) {
            YearMonth month = YearMonth.from(Instant.ofEpochSecond(trade.exitTimeSec()).atZone(ZoneOffset.UTC));
            pnlByMonth.merge(month, trade.netPnl(), Double::sum);
        }
        return pnlByMonth.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + fmt(entry.getValue()))
                .collect(Collectors.joining(";"));
    }

    private static String topMarket(List<ClosedTrade> trades) {
        return trades.stream()
                .collect(Collectors.groupingBy(ClosedTrade::market, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("");
    }

    private static double topMarketTradePct(List<ClosedTrade> trades) {
        if (trades.isEmpty()) {
            return 0;
        }
        long topCount = trades.stream()
                .collect(Collectors.groupingBy(ClosedTrade::market, Collectors.counting()))
                .values()
                .stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);
        return topCount * 100.0 / trades.size();
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private static String md(String value) {
        return value == null ? "" : value.replace("|", "\\|");
    }

    private static String fmt(double value) {
        if (Double.isInfinite(value)) {
            return value > 0 ? "Infinity" : "-Infinity";
        }
        if (Double.isNaN(value)) {
            return "NaN";
        }
        return String.format(Locale.US, "%.6f", value);
    }
}
