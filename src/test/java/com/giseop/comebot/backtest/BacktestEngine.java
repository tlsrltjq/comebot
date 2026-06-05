package com.giseop.comebot.backtest;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.market.service.BtcTrendCacheService;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.risk.domain.PositionExitPolicy;
import com.giseop.comebot.risk.service.PositionExitSignalService;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import com.giseop.comebot.strategy.candidate.CandidateScannerService;
import com.giseop.comebot.strategy.candidate.TradingCandidate;
import com.giseop.comebot.strategy.domain.TradingSignal;
import com.giseop.comebot.strategy.service.StrategyMarketSettingsService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Replays historical 1-minute candles through the unmodified operating strategy
 * ({@link CandidateScannerService}) and exit logic ({@link PositionExitSignalService}),
 * settling fills against the real {@link PaperPortfolioService}. Because the same
 * production code decides entries and exits, the resulting PF/win-rate are the
 * operating engine's numbers — not a re-implementation that can silently diverge
 * (which is the failure mode of the standalone {@code backtest.py}).
 *
 * <p>Entry model mirrors ADR-013 maker limit orders: on a SELECTED signal a limit
 * buy is placed at the signal candle close, valid {@code limitValidityMinutes},
 * filling when a later candle's low reaches the limit (never the same candle).
 * Exits are threshold-触발 taker market orders: TP/SL fill at the policy threshold
 * price adjusted for slippage. Trailing stops are not yet modelled intrabar — they
 * are off in current operations (ADR-011), so this matches today's config.
 */
final class BacktestEngine {

    private static final ExchangeMode EXCHANGE = ExchangeMode.UPBIT;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final List<CandleSeries> series;
    private final CandidateScannerService scanner;
    private final PositionExitSignalService exitSignalService;
    private final PaperPortfolioService portfolioService;
    private final BtcTrendCacheService btcTrendCache;
    private final ReplayCandleProvider provider;
    private final StrategyMarketSettingsService settings;
    private final BacktestConfig config;
    private final EntryGate entryGate;

    BacktestEngine(
            List<CandleSeries> series,
            CandidateScannerService scanner,
            PositionExitSignalService exitSignalService,
            PaperPortfolioService portfolioService,
            BtcTrendCacheService btcTrendCache,
            ReplayCandleProvider provider,
            StrategyMarketSettingsService settings,
            BacktestConfig config
    ) {
        this(series, scanner, exitSignalService, portfolioService, btcTrendCache,
                provider, settings, config, EntryGate.ALLOW_ALL);
    }

    BacktestEngine(
            List<CandleSeries> series,
            CandidateScannerService scanner,
            PositionExitSignalService exitSignalService,
            PaperPortfolioService portfolioService,
            BtcTrendCacheService btcTrendCache,
            ReplayCandleProvider provider,
            StrategyMarketSettingsService settings,
            BacktestConfig config,
            EntryGate entryGate
    ) {
        this.series = series;
        this.scanner = scanner;
        this.exitSignalService = exitSignalService;
        this.portfolioService = portfolioService;
        this.btcTrendCache = btcTrendCache;
        this.provider = provider;
        this.settings = settings;
        this.config = config;
        this.entryGate = entryGate;
    }

    Result run() {
        int marketCount = series.size();
        int[] pointer = new int[marketCount];
        Map<String, Pending> pendings = new HashMap<>();
        Map<String, OpenTrade> open = new HashMap<>();
        List<ClosedTrade> closed = new ArrayList<>();
        long[] stats = new long[3]; // [0]=signals, [1]=fills, [2]=expiries
        long lastRefreshHour = Long.MIN_VALUE;

        while (true) {
            long minClose = Long.MAX_VALUE;
            for (int m = 0; m < marketCount; m++) {
                if (pointer[m] < series.get(m).size()) {
                    minClose = Math.min(minClose, series.get(m).closeTimeSec(pointer[m]));
                }
            }
            if (minClose == Long.MAX_VALUE) {
                break;
            }

            Instant cursor = Instant.ofEpochSecond(minClose);
            provider.setCursor(cursor);

            if (btcTrendCache != null) {
                long hour = Math.floorDiv(minClose, 3600L);
                if (hour != lastRefreshHour) {
                    btcTrendCache.refresh();
                    lastRefreshHour = hour;
                }
            }

            List<Integer> active = new ArrayList<>();
            for (int m = 0; m < marketCount; m++) {
                if (pointer[m] < series.get(m).size()
                        && series.get(m).closeTimeSec(pointer[m]) == minClose) {
                    active.add(m);
                }
            }

            // PHASE A — exits and pending fills first, so freed slots are reusable this minute.
            for (int m : active) {
                CandleSeries s = series.get(m);
                int idx = pointer[m];
                if (open.containsKey(s.market())) {
                    tryExit(s, idx, cursor, open, closed);
                }
                if (pendings.containsKey(s.market())) {
                    tryFillOrExpire(s, idx, minClose, pendings, open, stats);
                }
            }

            // PHASE B — entries.
            int buysThisMinute = 0;
            for (int m : active) {
                CandleSeries s = series.get(m);
                String market = s.market();
                if (open.containsKey(market) || pendings.containsKey(market)) {
                    continue;
                }
                if (open.size() + pendings.size() >= config.maxOpenPositions()) {
                    continue;
                }
                if (buysThisMinute >= config.maxBuysPerMinute()) {
                    continue;
                }
                TradingCandidate candidate = scanner.scan(EXCHANGE, market);
                if (candidate.decision() != CandidateDecision.SELECTED) {
                    continue;
                }
                if (!entryGate.allows(market, s.closeTimeSec(pointer[m]))) {
                    continue; // blocked by regime filter
                }
                stats[0]++;
                if (config.marketEntry()) {
                    // Market entry: fill at the NEXT candle's open (backtest.py convention).
                    int next = pointer[m] + 1;
                    if (next < s.size() && s.open(next) > 0) {
                        double entryPrice = s.open(next);
                        BigDecimal qty = settings.buyQuantity(market, BigDecimal.valueOf(entryPrice));
                        long entryTime = s.closeTimeSec(pointer[m]);
                        applyOrder(market, OrderSide.BUY, qty, entryPrice, Instant.ofEpochSecond(entryTime));
                        open.put(market, new OpenTrade(entryTime, entryPrice, qty));
                        stats[1]++;
                        buysThisMinute++;
                    }
                } else {
                    // Maker limit entry at the signal candle close (ADR-013).
                    double limitPrice = s.close(pointer[m]);
                    pendings.put(market, new Pending(minClose, limitPrice));
                    buysThisMinute++;
                }
            }

            for (int m : active) {
                pointer[m]++;
            }
        }

        return new Result(closed, stats[0], stats[1], stats[2],
                config.trainTestSplitSec(), config.initialCapital());
    }

    private void tryExit(
            CandleSeries s,
            int idx,
            Instant cursor,
            Map<String, OpenTrade> open,
            List<ClosedTrade> closed
    ) {
        String market = s.market();
        OpenTrade trade = open.get(market);
        double low = s.low(idx);
        double high = s.high(idx);

        // Reuse the production exit policy: SL/abnormal detected via the low, TP via the high.
        Optional<TradingSignal> viaLow = exitSignalService.evaluate(
                EXCHANGE, new MarketPrice(market, BigDecimal.valueOf(low), cursor));
        Optional<TradingSignal> viaHigh = exitSignalService.evaluate(
                EXCHANGE, new MarketPrice(market, BigDecimal.valueOf(high), cursor));

        boolean stopLoss = viaLow.map(sig -> sig.reason().startsWith("Stop loss")).orElse(false);
        boolean takeProfit = viaHigh.map(sig -> sig.reason().startsWith("Take profit")).orElse(false);
        if (!stopLoss && !takeProfit) {
            return;
        }

        // Intrabar tie-break: when one 1m candle touches both TP and SL, the true
        // fill order is unknowable without tick data. Optimistic books the win
        // (backtest.py's convention), pessimistic books the loss. Running both
        // brackets the real outcome — and exposes how fragile the edge is to it.
        boolean chooseTakeProfit = (takeProfit && stopLoss) ? config.intrabarOptimistic() : takeProfit;

        PositionExitPolicy policy = exitSignalService.currentPolicy();
        String reason;
        double thresholdPrice;
        if (chooseTakeProfit) {
            reason = "TP";
            thresholdPrice = trade.entryPrice() * (1.0 + policy.takeProfitRate().doubleValue() / 100.0);
        } else {
            reason = "SL";
            thresholdPrice = trade.entryPrice() * (1.0 + policy.stopLossRate().doubleValue() / 100.0);
        }

        double exitFill = thresholdPrice * (1.0 - config.slippageRate()); // taker sell, adverse slippage
        closed.add(buildClosedTrade(market, trade, thresholdPrice, exitFill, s.closeTimeSec(idx), reason));
        applyOrder(market, OrderSide.SELL, trade.quantity(), exitFill, cursor);
        open.remove(market);
    }

    private void tryFillOrExpire(
            CandleSeries s,
            int idx,
            long minClose,
            Map<String, Pending> pendings,
            Map<String, OpenTrade> open,
            long[] stats
    ) {
        String market = s.market();
        Pending pending = pendings.get(market);
        if (minClose <= pending.createdSec()) {
            return; // never fill on the signal candle (capturedAt > createdAt)
        }
        if (minClose - pending.createdSec() > (long) config.limitValidityMinutes() * 60L) {
            pendings.remove(market);
            stats[2]++;
            return;
        }
        if (s.low(idx) <= pending.limitPrice()) {
            BigDecimal limit = BigDecimal.valueOf(pending.limitPrice());
            BigDecimal qty = settings.buyQuantity(market, limit);
            applyOrder(market, OrderSide.BUY, qty, pending.limitPrice(), Instant.ofEpochSecond(minClose));
            open.put(market, new OpenTrade(minClose, pending.limitPrice(), qty));
            pendings.remove(market);
            stats[1]++;
        }
    }

    private ClosedTrade buildClosedTrade(
            String market,
            OpenTrade trade,
            double grossExitPrice,
            double exitFill,
            long exitTimeSec,
            String reason
    ) {
        double qty = trade.quantity().doubleValue();
        double buyAmount = qty * trade.entryPrice();
        double feeBuy = buyAmount * config.makerFeeRate();
        double sellGross = qty * exitFill;
        double feeSell = sellGross * config.takerFeeRate();
        double netPnl = (sellGross - feeSell) - (buyAmount + feeBuy);
        // Gross = pre-fee, pre-slippage threshold P&L (backtest.py's pnl_raw basis).
        double grossPnl = qty * (grossExitPrice - trade.entryPrice());
        double costBasis = buyAmount + feeBuy;
        double returnPct = costBasis == 0 ? 0 : netPnl / costBasis * 100.0;
        return new ClosedTrade(
                market, trade.entryTimeSec(), exitTimeSec,
                trade.entryPrice(), exitFill, netPnl, grossPnl, returnPct, reason);
    }

    private void applyOrder(String market, OrderSide side, BigDecimal qty, double price, Instant at) {
        portfolioService.apply(EXCHANGE, new OrderResult(
                market, side, qty, BigDecimal.valueOf(price),
                OrderStatus.FILLED, "backtest", at));
    }

    private record Pending(long createdSec, double limitPrice) {
    }

    private record OpenTrade(long entryTimeSec, double entryPrice, BigDecimal quantity) {
    }

    /** Outcome of a run: closed trades plus entry funnel stats and the OOS split boundary. */
    record Result(
            List<ClosedTrade> closed,
            long signals,
            long fills,
            long expiries,
            long trainTestSplitSec,
            double initialCapital
    ) {

        double fillRatePct() {
            return signals == 0 ? 0 : fills * 100.0 / signals;
        }

        BacktestReport full() {
            return BacktestReport.of("ALL", closed, initialCapital);
        }

        BacktestReport train() {
            return BacktestReport.of("TRAIN", closed.stream()
                    .filter(t -> t.exitTimeSec() < trainTestSplitSec).toList(), initialCapital);
        }

        BacktestReport test() {
            return BacktestReport.of("TEST", closed.stream()
                    .filter(t -> t.exitTimeSec() >= trainTestSplitSec).toList(), initialCapital);
        }
    }
}
