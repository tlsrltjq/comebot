package com.giseop.comebot.market.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.giseop.comebot.market.dto.MarketSentimentSnapshot;
import com.giseop.comebot.market.dto.SentimentSignal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 시장 심리 데이터 집계 서비스.
 *
 * <p>무료 공개 API 사용:
 * <ul>
 *   <li>CoinGecko — 전체 시가총액, BTC 도미넌스, 스테이블코인 시총</li>
 *   <li>Binance Futures — 펀딩레이트, OI 현재/4h 변화, 롱숏 비율</li>
 * </ul>
 *
 * <p>미지원 (유료 API 필요): 거래소 입출금 흐름, 전체 청산 규모
 */
@Service
public class MarketSentimentService {

    private static final Logger log = LoggerFactory.getLogger(MarketSentimentService.class);

    private final RestClient coingecko;
    private final RestClient binanceFutures;
    private final AtomicReference<MarketSentimentSnapshot> cache =
            new AtomicReference<>(MarketSentimentSnapshot.empty());

    public MarketSentimentService() {
        this.coingecko = RestClient.builder().baseUrl("https://api.coingecko.com").build();
        this.binanceFutures = RestClient.builder().baseUrl("https://fapi.binance.com").build();
    }

    MarketSentimentService(RestClient coingecko, RestClient binanceFutures) {
        this.coingecko = coingecko;
        this.binanceFutures = binanceFutures;
    }

    public MarketSentimentSnapshot latest() {
        return cache.get();
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 5_000)
    public void refresh() {
        try {
            cache.set(fetch());
        } catch (Exception e) {
            log.warn("[SENTIMENT] refresh failed, using cached data. error={}", e.getMessage());
        }
    }

    // ── API Response DTOs ────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CoinGeckoGlobalWrapper(GlobalData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GlobalData(
            @JsonProperty("total_market_cap") Map<String, Double> totalMarketCap,
            @JsonProperty("market_cap_change_percentage_24h_usd") double marketCapChange24h,
            @JsonProperty("market_cap_percentage") Map<String, Double> marketCapPercentage
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StableCoin(
            @JsonProperty("market_cap") double marketCap,
            @JsonProperty("price_change_percentage_24h") Double priceChange24h
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PremiumIndex(
            @JsonProperty("lastFundingRate") String lastFundingRate
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OiCurrent(
            @JsonProperty("openInterest") String openInterest
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OiHist(
            @JsonProperty("sumOpenInterest") String sumOpenInterest
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LongShortRatio(
            @JsonProperty("longShortRatio") String longShortRatio
    ) {
    }

    // ── fetch ────────────────────────────────────────────────────────────────

    private MarketSentimentSnapshot fetch() {
        double totalMcapB = 0;
        double mcapChange24h = 0;
        double btcDom = 0;
        double stableMcapB = 0;
        double stableChange24h = 0;
        double btcFunding = 0;
        double ethFunding = 0;
        double btcOiThousands = 0;
        double btcOiChange4h = 0;
        double lsRatio = 1.0;

        // ── CoinGecko global ─────────────────────────────────────────────────
        try {
            CoinGeckoGlobalWrapper resp = coingecko.get()
                    .uri("/api/v3/global")
                    .retrieve()
                    .body(CoinGeckoGlobalWrapper.class);
            if (resp != null && resp.data() != null) {
                GlobalData d = resp.data();
                totalMcapB = d.totalMarketCap() != null
                        ? d.totalMarketCap().getOrDefault("usd", 0.0) / 1e9 : 0;
                mcapChange24h = d.marketCapChange24h();
                btcDom = d.marketCapPercentage() != null
                        ? d.marketCapPercentage().getOrDefault("btc", 0.0) : 0;
            }
        } catch (RestClientException e) {
            log.warn("[SENTIMENT] CoinGecko global failed: {}", e.getMessage());
        }

        // ── CoinGecko stablecoins ────────────────────────────────────────────
        try {
            List<StableCoin> coins = coingecko.get()
                    .uri("/api/v3/coins/markets?vs_currency=usd&category=stablecoins"
                            + "&order=market_cap_desc&per_page=5&page=1")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<StableCoin>>() { });
            if (coins != null && !coins.isEmpty()) {
                double totalCap = 0;
                double weightedChange = 0;
                for (StableCoin c : coins) {
                    double chg = c.priceChange24h() != null ? c.priceChange24h() : 0.0;
                    totalCap += c.marketCap();
                    weightedChange += chg * c.marketCap();
                }
                stableMcapB = totalCap / 1e9;
                stableChange24h = totalCap > 0 ? weightedChange / totalCap : 0;
            }
        } catch (RestClientException e) {
            log.warn("[SENTIMENT] CoinGecko stablecoins failed: {}", e.getMessage());
        }

        // ── Binance: funding rates ────────────────────────────────────────────
        btcFunding = fundingRate("BTCUSDT");
        ethFunding = fundingRate("ETHUSDT");

        // ── Binance: OI current ──────────────────────────────────────────────
        try {
            OiCurrent oi = binanceFutures.get()
                    .uri("/fapi/v1/openInterest?symbol=BTCUSDT")
                    .retrieve()
                    .body(OiCurrent.class);
            if (oi != null && oi.openInterest() != null) {
                btcOiThousands = Double.parseDouble(oi.openInterest()) / 1_000.0;
            }
        } catch (RestClientException e) {
            log.warn("[SENTIMENT] Binance OI failed: {}", e.getMessage());
        }

        // ── Binance: OI 4h history ───────────────────────────────────────────
        try {
            List<OiHist> hist = binanceFutures.get()
                    .uri("/futures/data/openInterestHist?symbol=BTCUSDT&period=4h&limit=2")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<OiHist>>() { });
            if (hist != null && hist.size() >= 2) {
                double older = Double.parseDouble(hist.get(0).sumOpenInterest());
                double newer = Double.parseDouble(hist.get(hist.size() - 1).sumOpenInterest());
                if (older > 0) {
                    btcOiChange4h = (newer - older) / older * 100.0;
                }
            }
        } catch (RestClientException e) {
            log.warn("[SENTIMENT] Binance OI hist failed: {}", e.getMessage());
        }

        // ── Binance: top trader L/S ratio ────────────────────────────────────
        try {
            List<LongShortRatio> ls = binanceFutures.get()
                    .uri("/futures/data/topLongShortPositionRatio?symbol=BTCUSDT&period=1h&limit=1")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<LongShortRatio>>() { });
            if (ls != null && !ls.isEmpty() && ls.get(0).longShortRatio() != null) {
                lsRatio = Double.parseDouble(ls.get(0).longShortRatio());
            }
        } catch (RestClientException e) {
            log.warn("[SENTIMENT] Binance L/S failed: {}", e.getMessage());
        }

        // ── score ────────────────────────────────────────────────────────────
        List<SentimentSignal> signals = buildSignals(
                mcapChange24h, stableChange24h, btcFunding, btcOiChange4h, lsRatio);
        int score = signals.stream().mapToInt(SentimentSignal::score).sum();
        String label = score >= 4 ? "RISK_ON" : score <= -4 ? "RISK_OFF" : "NEUTRAL";

        log.info("[SENTIMENT] score={} label={} mcap24h={}% btcFunding={} OI4h={}% LS={}",
                score, label,
                String.format("%.2f", mcapChange24h),
                String.format("%.6f", btcFunding),
                String.format("%.2f", btcOiChange4h),
                String.format("%.3f", lsRatio));

        return new MarketSentimentSnapshot(
                Instant.now(),
                totalMcapB, mcapChange24h, btcDom,
                stableMcapB, stableChange24h,
                btcFunding * 100, ethFunding * 100,
                btcOiThousands, btcOiChange4h,
                lsRatio,
                "유료 API 필요 (Glassnode / CryptoQuant)",
                "유료 API 필요 (CoinGlass)",
                score, label, signals
        );
    }

    private double fundingRate(String symbol) {
        try {
            PremiumIndex resp = binanceFutures.get()
                    .uri("/fapi/v1/premiumIndex?symbol=" + symbol)
                    .retrieve()
                    .body(PremiumIndex.class);
            return resp != null && resp.lastFundingRate() != null
                    ? Double.parseDouble(resp.lastFundingRate()) : 0.0;
        } catch (RestClientException e) {
            log.warn("[SENTIMENT] funding rate {} failed: {}", symbol, e.getMessage());
            return 0.0;
        }
    }

    // ── scoring ──────────────────────────────────────────────────────────────

    private List<SentimentSignal> buildSignals(
            double mcapChange, double stableChange,
            double btcFunding, double oiChange4h, double lsRatio) {

        List<SentimentSignal> list = new ArrayList<>();

        int s1 = mcapChange > 5 ? 2 : mcapChange > 1 ? 1 : mcapChange < -5 ? -2 : mcapChange < -1 ? -1 : 0;
        list.add(SentimentSignal.of("시가총액 24h 변화",
                String.format("%+.2f%%", mcapChange),
                "전체 암호화폐 시장 시가총액 24시간 변화율", s1));

        int s2 = stableChange < -1 ? 2 : stableChange < 0 ? 1 : stableChange > 1 ? -2 : stableChange > 0 ? -1 : 0;
        list.add(SentimentSignal.of("스테이블코인 흐름",
                String.format("%+.2f%%", stableChange),
                "스테이블 감소=코인 매수(Risk-on), 증가=안전 피신(Risk-off)", s2));

        double btcFundingPct = btcFunding * 100;
        int s3 = btcFundingPct > 0.03 ? 2 : btcFundingPct > 0.01 ? 1
                : btcFundingPct < -0.03 ? -2 : btcFundingPct < -0.01 ? -1 : 0;
        list.add(SentimentSignal.of("BTC 펀딩레이트",
                String.format("%+.4f%%", btcFundingPct),
                "양수=롱 우세(강세), 음수=숏 우세(약세)", s3));

        int s4 = oiChange4h > 3 ? 1 : oiChange4h < -3 ? -1 : 0;
        list.add(SentimentSignal.of("BTC OI 4h 변화",
                String.format("%+.2f%%", oiChange4h),
                "미결제약정 증가=신규 자금, 감소=포지션 청산", s4));

        int s5 = lsRatio > 1.5 ? 1 : lsRatio < 0.8 ? -1 : 0;
        list.add(SentimentSignal.of("상위 트레이더 L/S 비율",
                String.format("%.3f", lsRatio),
                "1.0 초과=롱 우세, 미만=숏 우세 (Binance 상위 트레이더)", s5));

        list.add(SentimentSignal.unavailable("거래소 입출금 흐름", "유료 API 필요 (Glassnode/CryptoQuant)"));
        list.add(SentimentSignal.unavailable("청산 규모 (전 거래소)", "유료 API 필요 (CoinGlass)"));

        return list;
    }
}
