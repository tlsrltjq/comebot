package com.giseop.comebot.market.dto;

import java.time.Instant;
import java.util.List;

public record MarketSentimentSnapshot(
        Instant refreshedAt,

        // ── 시가총액 ──────────────────────────────────────────────────
        double totalMarketCapBillionUsd,
        double totalMarketCapChange24hPct,
        double btcDominancePct,

        // ── 스테이블코인 ──────────────────────────────────────────────
        double stablecoinMarketCapBillionUsd,
        double stablecoinChange24hPct,

        // ── 선물 Funding Rate ─────────────────────────────────────────
        double btcFundingRatePct,
        double ethFundingRatePct,

        // ── Open Interest ─────────────────────────────────────────────
        double btcOpenInterestBillionUsd,
        double btcOiChange4hPct,

        // ── 롱/숏 비율 (상위 트레이더) ─────────────────────────────────
        double btcLongShortRatio,

        // ── 미지원 항목 ───────────────────────────────────────────────
        String exchangeFlowNote,
        String liquidationNote,

        // ── Risk Score ────────────────────────────────────────────────
        int riskScore,
        String riskLabel,
        List<SentimentSignal> signals
) {
    public static MarketSentimentSnapshot empty() {
        return new MarketSentimentSnapshot(
                Instant.now(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                "유료 API 필요 (Glassnode / CryptoQuant)",
                "유료 API 필요 (CoinGlass)",
                0, "NEUTRAL", List.of()
        );
    }
}
