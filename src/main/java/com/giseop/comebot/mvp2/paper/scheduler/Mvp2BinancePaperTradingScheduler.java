package com.giseop.comebot.mvp2.paper.scheduler;

import com.giseop.comebot.mvp2.paper.Mvp2PaperTradingProperties;
import com.giseop.comebot.mvp2.paper.Mvp2PaperTradingService;
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
        if (!properties.isBinanceSchedulerEnabled()) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            log.warn("MVP2 Binance paper scheduler skipped because previous run is still active");
            return;
        }
        try {
            tradingService.runBinanceSymbols();
        } catch (RuntimeException exception) {
            log.warn("MVP2 Binance paper scheduler failed", exception);
        } finally {
            running.set(false);
        }
    }
}
