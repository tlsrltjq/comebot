package com.giseop.comebot.mvp2.paper.scheduler;

import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.mvp2.exchange.Exchange;
import com.giseop.comebot.mvp2.paper.Mvp2PaperTradingProperties;
import com.giseop.comebot.mvp2.paper.Mvp2PaperTradingResult;
import com.giseop.comebot.mvp2.paper.Mvp2PaperTradingService;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Mvp2BinancePaperTradingScheduler {

    private static final Logger log = LoggerFactory.getLogger(Mvp2BinancePaperTradingScheduler.class);

    private final Mvp2PaperTradingProperties properties;
    private final Mvp2PaperTradingService tradingService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public Mvp2BinancePaperTradingScheduler(
            Mvp2PaperTradingProperties properties,
            Mvp2PaperTradingService tradingService
    ) {
        this.properties = properties;
        this.tradingService = tradingService;
    }

    @Scheduled(fixedDelayString = "${mvp2.paper.binance-scheduler-fixed-delay-ms:30000}")
    public void run() {
        runOnce();
    }

    public Mvp2PaperSchedulerRunSummary runOnce() {
        Instant ranAt = Instant.now();
        if (!properties.isBinanceSchedulerEnabled()) {
            return new Mvp2PaperSchedulerRunSummary(false, 0, 0, 0, 0, 0, 0, false, ranAt);
        }
        if (!running.compareAndSet(false, true)) {
            log.warn("MVP2 Binance paper scheduler skipped because previous run is still active");
            return new Mvp2PaperSchedulerRunSummary(true, 0, 0, 0, 0, 0, 0, true, ranAt);
        }
        try {
            return runConfiguredSymbols(ranAt);
        } finally {
            running.set(false);
        }
    }

    private Mvp2PaperSchedulerRunSummary runConfiguredSymbols(Instant ranAt) {
        List<String> symbols = properties.getBinanceSymbols().stream()
                .filter(symbol -> symbol != null && !symbol.isBlank())
                .toList();
        int executedSymbols = 0;
        int buyCount = 0;
        int sellCount = 0;
        int holdCount = 0;
        int failedCount = 0;

        for (String symbol : symbols) {
            try {
                Mvp2PaperTradingResult result = tradingService.run(Exchange.BINANCE, symbol);
                executedSymbols++;
                if (result.side() == OrderSide.BUY) {
                    buyCount++;
                } else if (result.side() == OrderSide.SELL) {
                    sellCount++;
                } else {
                    holdCount++;
                }
            } catch (RuntimeException exception) {
                failedCount++;
                log.warn("MVP2 Binance paper scheduler failed for symbol={}", symbol, exception);
            }
        }

        return new Mvp2PaperSchedulerRunSummary(
                true,
                symbols.size(),
                executedSymbols,
                buyCount,
                sellCount,
                holdCount,
                failedCount,
                false,
                ranAt
        );
    }
}
