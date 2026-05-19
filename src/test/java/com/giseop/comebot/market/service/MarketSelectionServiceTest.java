package com.giseop.comebot.market.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.dto.BinanceUsdtTickerResponse;
import com.giseop.comebot.market.dto.UpbitKrwTickerResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketSelectionServiceTest {

    private final UpbitKrwTickerStore tickerStore = new UpbitKrwTickerStore();
    private final BinanceUsdtTickerStore binanceTickerStore = new BinanceUsdtTickerStore();
    private final MarketSelectionService service = new MarketSelectionService(tickerStore, binanceTickerStore);

    @Test
    void allKrwResolvesToLatestTickerMarketsByTradeAmount() {
        tickerStore.replace(List.of(
                ticker("KRW-ETH", "100"),
                ticker("BTC-USDT"),
                ticker("KRW-XRP", "200"),
                ticker("KRW-BTC", "300")
        ));

        assertThat(service.resolve(List.of("ALL_KRW"))).containsExactly("KRW-BTC", "KRW-XRP", "KRW-ETH");
    }

    @Test
    void allKrwUsesTopKrwMarketLimitFromProperties() {
        tickerStore.replace(java.util.stream.IntStream.rangeClosed(1, 25)
                .mapToObj(index -> ticker("KRW-COIN" + index, String.valueOf(index)))
                .toList());

        assertThat(service.resolve(List.of("ALL_KRW"))).hasSize(20);
        assertThat(service.resolve(List.of("ALL_KRW")).getFirst()).isEqualTo("KRW-COIN25");
    }

    @Test
    void allKrwAllowsAnyKrwMarketForRiskValidation() {
        assertThat(service.isAllowed("KRW-XRP", List.of("ALL_KRW"))).isTrue();
        assertThat(service.isAllowed("BTC-XRP", List.of("ALL_KRW"))).isFalse();
    }

    @Test
    void allUsdtResolvesToLatestBinanceSymbolsByQuoteVolume() {
        binanceTickerStore.replace(List.of(
                binanceTicker("ETHUSDT", "200"),
                binanceTicker("ETHBTC", "999"),
                binanceTicker("XRPUSDT", "100"),
                binanceTicker("BTCUSDT", "300")
        ));

        assertThat(service.resolve(List.of("ALL_USDT"))).containsExactly("BTCUSDT", "ETHUSDT", "XRPUSDT");
    }

    @Test
    void allUsdtUsesTopUsdtSymbolLimitFromProperties() {
        binanceTickerStore.replace(java.util.stream.IntStream.rangeClosed(1, 35)
                .mapToObj(index -> binanceTicker("COIN" + index + "USDT", String.valueOf(index)))
                .toList());

        assertThat(service.resolve(List.of("ALL_USDT"))).hasSize(30);
        assertThat(service.resolve(List.of("ALL_USDT")).getFirst()).isEqualTo("COIN35USDT");
    }

    @Test
    void allUsdtAllowsAnyUsdtSymbolForRiskValidation() {
        assertThat(service.isAllowed("BTCUSDT", List.of("ALL_USDT"))).isTrue();
        assertThat(service.isAllowed("ETHBTC", List.of("ALL_USDT"))).isFalse();
    }

    @Test
    void exchangeAwareResolveKeepsOnlyMarketsForThatExchange() {
        assertThat(service.resolve(ExchangeMode.UPBIT, List.of("KRW-BTC", "BTCUSDT", "ALL_USDT")))
                .containsExactly("KRW-BTC");
        assertThat(service.resolve(ExchangeMode.BINANCE, List.of("KRW-BTC", "BTCUSDT", "ALL_KRW")))
                .containsExactly("BTCUSDT");
    }

    @Test
    void exchangeAwareResolveSupportsBothAllTokensInOneConfig() {
        tickerStore.replace(List.of(
                ticker("KRW-ETH", "100"),
                ticker("KRW-BTC", "300")
        ));
        binanceTickerStore.replace(List.of(
                binanceTicker("ETHUSDT", "200"),
                binanceTicker("BTCUSDT", "300")
        ));

        assertThat(service.resolve(ExchangeMode.UPBIT, List.of("ALL_KRW", "ALL_USDT")))
                .containsExactly("KRW-BTC", "KRW-ETH");
        assertThat(service.resolve(ExchangeMode.BINANCE, List.of("ALL_KRW", "ALL_USDT")))
                .containsExactly("BTCUSDT", "ETHUSDT");
    }

    private UpbitKrwTickerResponse ticker(String market) {
        return new UpbitKrwTickerResponse(market, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN);
    }

    private UpbitKrwTickerResponse ticker(String market, String tradeAmount24h) {
        return new UpbitKrwTickerResponse(market, BigDecimal.ONE, BigDecimal.ZERO, new BigDecimal(tradeAmount24h));
    }

    private BinanceUsdtTickerResponse binanceTicker(String symbol, String quoteVolume) {
        return new BinanceUsdtTickerResponse(symbol, BigDecimal.ONE, new BigDecimal(quoteVolume));
    }
}
