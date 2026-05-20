package com.giseop.comebot.strategy.dto;

import com.giseop.comebot.strategy.candidate.CandidateDecision;
import com.giseop.comebot.strategy.candidate.TradingCandidate;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import java.math.BigDecimal;
import java.time.Instant;

public record TradingCandidateResponse(
        String market,
        CandidateDecision decision,
        String reason,
        BigDecimal currentPrice,
        BigDecimal priceChangeRate,
        BigDecimal highLowRangeRate,
        BigDecimal tradeAmountChangeRate,
        MarketTrend trend,
        Boolean lastCandleBullish,
        Instant scannedAt,
        CandidateReasonType reasonType,
        CandidateRiskReasonType riskReasonType
) {

    public static TradingCandidateResponse from(TradingCandidate candidate) {
        CandidateReasonType reasonType = reasonType(candidate.reason());
        return new TradingCandidateResponse(
                candidate.market(),
                candidate.decision(),
                candidate.reason(),
                candidate.currentPrice(),
                candidate.priceChangeRate(),
                candidate.highLowRangeRate(),
                candidate.tradeAmountChangeRate(),
                candidate.trend(),
                candidate.lastCandleBullish(),
                candidate.scannedAt(),
                reasonType,
                riskReasonType(reasonType)
        );
    }

    private static CandidateReasonType reasonType(String reason) {
        if (reason == null || reason.isBlank()) {
            return CandidateReasonType.OTHER;
        }
        return switch (reason) {
            case "Volatility long candidate selected" -> CandidateReasonType.SELECTED;
            case "Trend is not UP" -> CandidateReasonType.TREND_NOT_UP;
            case "Price change rate is below threshold" -> CandidateReasonType.PRICE_CHANGE_BELOW_THRESHOLD;
            case "Trade amount change rate is below threshold" -> CandidateReasonType.TRADE_AMOUNT_CHANGE_BELOW_THRESHOLD;
            case "Price change rate is overheated" -> CandidateReasonType.PRICE_CHANGE_OVERHEATED;
            case "High low range rate is overheated" -> CandidateReasonType.HIGH_LOW_RANGE_OVERHEATED;
            case "Not enough valid trade amount candles" -> CandidateReasonType.NOT_ENOUGH_VALID_TRADE_AMOUNT;
            default -> reasonTypeFromPrefix(reason);
        };
    }

    private static CandidateReasonType reasonTypeFromPrefix(String reason) {
        if (reason.startsWith("Candidate scan failed:")) {
            return CandidateReasonType.SCAN_FAILED;
        }
        if (reason.startsWith("Market concentration exceeds block exposure rate:")) {
            return CandidateReasonType.CONCENTRATION_RISK;
        }
        if (reason.startsWith("Stop loss cooldown active:")) {
            return CandidateReasonType.STOP_LOSS_COOLDOWN;
        }
        return CandidateReasonType.OTHER;
    }

    private static CandidateRiskReasonType riskReasonType(CandidateReasonType reasonType) {
        return switch (reasonType) {
            case CONCENTRATION_RISK -> CandidateRiskReasonType.CONCENTRATION;
            case STOP_LOSS_COOLDOWN -> CandidateRiskReasonType.STOP_LOSS_COOLDOWN;
            default -> CandidateRiskReasonType.NONE;
        };
    }
}
