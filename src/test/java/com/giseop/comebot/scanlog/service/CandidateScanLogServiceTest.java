package com.giseop.comebot.scanlog.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.scanlog.domain.CandidateScanLog;
import com.giseop.comebot.scanlog.repository.InMemoryCandidateScanLogRepository;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import com.giseop.comebot.strategy.candidate.TradingCandidate;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CandidateScanLogServiceTest {

    private final InMemoryCandidateScanLogRepository repository = new InMemoryCandidateScanLogRepository();
    private final CandidateScanLogService service = new CandidateScanLogService(repository);

    @Test
    void saveMapsAllFieldsFromCandidate() {
        Instant scannedAt = Instant.parse("2026-05-01T00:00:00Z");
        TradingCandidate candidate = new TradingCandidate(
                "KRW-BTC",
                CandidateDecision.SELECTED,
                "Volatility long candidate selected",
                new BigDecimal("100"),
                new BigDecimal("2.5"),
                new BigDecimal("5"),
                new BigDecimal("30"),
                MarketTrend.UP,
                true,
                scannedAt
        );

        CandidateScanLog log = service.save(ExchangeMode.UPBIT, candidate);

        assertThat(log.id()).isNotBlank();
        assertThat(log.exchange()).isEqualTo(ExchangeMode.UPBIT);
        assertThat(log.market()).isEqualTo("KRW-BTC");
        assertThat(log.decision()).isEqualTo(CandidateDecision.SELECTED);
        assertThat(log.reason()).isEqualTo("Volatility long candidate selected");
        assertThat(log.currentPrice()).isEqualByComparingTo("100");
        assertThat(log.priceChangeRate()).isEqualByComparingTo("2.5");
        assertThat(log.highLowRangeRate()).isEqualByComparingTo("5");
        assertThat(log.tradeAmountChangeRate()).isEqualByComparingTo("30");
        assertThat(log.trend()).isEqualTo(MarketTrend.UP);
        assertThat(log.lastCandleBullish()).isTrue();
        assertThat(log.scannedAt()).isEqualTo(scannedAt);
    }

    @Test
    void saveUsesUpbitAsDefaultWhenExchangeIsNull() {
        TradingCandidate candidate = skippedCandidate("KRW-ETH");

        CandidateScanLog log = service.save(null, candidate);

        assertThat(log.exchange()).isEqualTo(ExchangeMode.UPBIT);
    }

    @Test
    void saveUsesCurrentTimeWhenCandidateScannedAtIsNull() {
        TradingCandidate candidate = new TradingCandidate(
                "KRW-BTC", CandidateDecision.SKIPPED, "reason",
                null, null, null, null, null, null, null
        );

        CandidateScanLog log = service.save(ExchangeMode.UPBIT, candidate);

        assertThat(log.scannedAt()).isNotNull();
    }

    @Test
    void findSinceReturnsLogsAfterFromInstant() {
        Instant from = Instant.parse("2026-05-01T10:00:00Z");
        service.save(ExchangeMode.UPBIT, candidateAt("KRW-BTC", Instant.parse("2026-05-01T09:59:00Z")));
        service.save(ExchangeMode.UPBIT, candidateAt("KRW-ETH", Instant.parse("2026-05-01T10:00:00Z")));
        service.save(ExchangeMode.UPBIT, candidateAt("KRW-XRP", Instant.parse("2026-05-01T10:01:00Z")));

        List<CandidateScanLog> result = service.findSince(ExchangeMode.UPBIT, from);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CandidateScanLog::market)
                .containsExactlyInAnyOrder("KRW-ETH", "KRW-XRP");
    }

    @Test
    void findSinceFiltersOutOtherExchange() {
        Instant from = Instant.parse("2026-05-01T10:00:00Z");
        service.save(ExchangeMode.UPBIT, candidateAt("KRW-BTC", from));
        service.save(ExchangeMode.BINANCE, candidateAt("BTCUSDT", from));

        List<CandidateScanLog> result = service.findSince(ExchangeMode.UPBIT, from);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().market()).isEqualTo("KRW-BTC");
    }

    @Test
    void findSinceWithDecisionFiltersDecision() {
        Instant from = Instant.parse("2026-05-01T10:00:00Z");
        service.save(ExchangeMode.UPBIT, selectedCandidateAt("KRW-BTC", from));
        service.save(ExchangeMode.UPBIT, candidateAt("KRW-ETH", from));

        List<CandidateScanLog> result = service.findSince(ExchangeMode.UPBIT, from, CandidateDecision.SELECTED);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().market()).isEqualTo("KRW-BTC");
    }

    private TradingCandidate skippedCandidate(String market) {
        return candidateAt(market, Instant.now());
    }

    private TradingCandidate candidateAt(String market, Instant scannedAt) {
        return new TradingCandidate(
                market, CandidateDecision.SKIPPED, "Trend is not UP",
                new BigDecimal("100"), new BigDecimal("-1"), new BigDecimal("3"),
                new BigDecimal("10"), MarketTrend.DOWN, false, scannedAt
        );
    }

    private TradingCandidate selectedCandidateAt(String market, Instant scannedAt) {
        return new TradingCandidate(
                market, CandidateDecision.SELECTED, "Volatility long candidate selected",
                new BigDecimal("100"), new BigDecimal("2.5"), new BigDecimal("5"),
                new BigDecimal("30"), MarketTrend.UP, true, scannedAt
        );
    }
}
