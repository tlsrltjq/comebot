package com.giseop.comebot.market.service;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.domain.TickerSnapshot;
import com.giseop.comebot.market.dto.MarketFlowEntry;
import com.giseop.comebot.market.dto.MarketFlowSummary;
import com.giseop.comebot.scanlog.service.CandidateScanLogService;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MarketFlowService {

    private static final int TOP_N = 30;
    private static final String BTC_KRW = "KRW-BTC";
    private static final String BTC_USDT = "BTCUSDT";

    private final TickerSnapshotStore tickerSnapshotStore;
    private final CandidateScanLogService candidateScanLogService;

    // previous rank snapshot: market → rank (per exchange)
    private final ConcurrentHashMap<String, Integer> prevRanksUpbit = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> prevRanksBinance = new ConcurrentHashMap<>();

    public MarketFlowService(
            TickerSnapshotStore tickerSnapshotStore,
            CandidateScanLogService candidateScanLogService
    ) {
        this.tickerSnapshotStore = tickerSnapshotStore;
        this.candidateScanLogService = candidateScanLogService;
    }

    public MarketFlowSummary summary(ExchangeMode exchange) {
        List<TickerSnapshot> snapshots = tickerSnapshotStore.findAll(exchange).stream()
                .filter(s -> s.accTradePrice24h() != null && s.accTradePrice24h().signum() > 0)
                .toList();

        if (snapshots.isEmpty()) {
            return new MarketFlowSummary(exchange, 0.0, 0.0, List.of());
        }

        // total 24h volume for share calculation
        double total = snapshots.stream()
                .mapToDouble(s -> s.accTradePrice24h().doubleValue())
                .sum();

        // candidate selection count in last 24h per market
        Map<String, Long> selectedCounts = selectedCountsLast24h(exchange);

        // sort by 24h volume descending, take TOP_N
        List<TickerSnapshot> sorted = snapshots.stream()
                .sorted(Comparator.comparing(TickerSnapshot::accTradePrice24h).reversed())
                .limit(TOP_N)
                .toList();

        Map<String, Integer> prevRanks = prevRanksFor(exchange);
        List<MarketFlowEntry> entries = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            TickerSnapshot s = sorted.get(i);
            int rank = i + 1;
            int prev = prevRanks.getOrDefault(s.market(), rank);
            double sharePct = total > 0 ? s.accTradePrice24h().doubleValue() / total * 100.0 : 0.0;
            int cnt = selectedCounts.getOrDefault(s.market(), 0L).intValue();
            entries.add(new MarketFlowEntry(s.market(), rank, prev, s.tradePrice(),
                    s.accTradePrice24h(), sharePct, cnt));
        }

        // persist current ranks as prev for next call
        Map<String, Integer> newRanks = entries.stream()
                .collect(Collectors.toMap(MarketFlowEntry::market, MarketFlowEntry::rank));
        prevRanks.clear();
        prevRanks.putAll(newRanks);

        // BTC dominance
        String btcMarket = exchange == ExchangeMode.BINANCE ? BTC_USDT : BTC_KRW;
        double btcVol = snapshots.stream()
                .filter(s -> btcMarket.equals(s.market()))
                .mapToDouble(s -> s.accTradePrice24h().doubleValue())
                .findFirst().orElse(0.0);
        double btcDominance = total > 0 ? btcVol / total * 100.0 : 0.0;

        // top-10 volume concentration
        double top10Vol = sorted.stream().limit(10)
                .mapToDouble(s -> s.accTradePrice24h().doubleValue()).sum();
        double top10Pct = total > 0 ? top10Vol / total * 100.0 : 0.0;

        return new MarketFlowSummary(exchange, btcDominance, top10Pct, entries);
    }

    private Map<String, Long> selectedCountsLast24h(ExchangeMode exchange) {
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        try {
            return candidateScanLogService.findSince(exchange, since, CandidateDecision.SELECTED)
                    .stream()
                    .filter(log -> log.market() != null)
                    .collect(Collectors.groupingBy(
                            com.giseop.comebot.scanlog.domain.CandidateScanLog::market,
                            Collectors.counting()
                    ));
        } catch (RuntimeException e) {
            return Map.of();
        }
    }

    private Map<String, Integer> prevRanksFor(ExchangeMode exchange) {
        return exchange == ExchangeMode.BINANCE ? prevRanksBinance : prevRanksUpbit;
    }
}
