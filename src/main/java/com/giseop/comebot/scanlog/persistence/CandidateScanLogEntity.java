package com.giseop.comebot.scanlog.persistence;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.scanlog.domain.CandidateScanLog;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "candidate_scan_log")
public class CandidateScanLogEntity {

    @Id
    @Column(nullable = false, length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExchangeMode exchange;

    @Column(nullable = false, length = 50)
    private String market;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CandidateDecision decision;

    @Column(length = 500)
    private String reason;

    @Column(name = "current_price", precision = 19, scale = 8)
    private BigDecimal currentPrice;

    @Column(name = "price_change_rate", precision = 19, scale = 4)
    private BigDecimal priceChangeRate;

    @Column(name = "high_low_range_rate", precision = 19, scale = 4)
    private BigDecimal highLowRangeRate;

    @Column(name = "trade_amount_change_rate", precision = 19, scale = 4)
    private BigDecimal tradeAmountChangeRate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MarketTrend trend;

    @Column(name = "last_candle_bullish")
    private Boolean lastCandleBullish;

    @Column(name = "scanned_at", nullable = false)
    private Instant scannedAt;

    protected CandidateScanLogEntity() {
    }

    public static CandidateScanLogEntity from(CandidateScanLog log) {
        CandidateScanLogEntity entity = new CandidateScanLogEntity();
        entity.id = log.id();
        entity.exchange = log.exchange();
        entity.market = log.market();
        entity.decision = log.decision();
        entity.reason = log.reason();
        entity.currentPrice = log.currentPrice();
        entity.priceChangeRate = log.priceChangeRate();
        entity.highLowRangeRate = log.highLowRangeRate();
        entity.tradeAmountChangeRate = log.tradeAmountChangeRate();
        entity.trend = log.trend();
        entity.lastCandleBullish = log.lastCandleBullish();
        entity.scannedAt = log.scannedAt();
        return entity;
    }

    public CandidateScanLog toDomain() {
        return new CandidateScanLog(
                id, exchange, market, decision, reason,
                currentPrice, priceChangeRate, highLowRangeRate, tradeAmountChangeRate,
                trend, lastCandleBullish, scannedAt
        );
    }
}
