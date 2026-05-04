package com.giseop.comebot.market.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.market.dto.UpbitKrwTickerResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketSelectionServiceTest {

    private final UpbitKrwTickerStore tickerStore = new UpbitKrwTickerStore();
    private final MarketSelectionService service = new MarketSelectionService(tickerStore);

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
    void allKrwUsesTopFiftyMarkets() {
        tickerStore.replace(java.util.stream.IntStream.rangeClosed(1, 55)
                .mapToObj(index -> ticker("KRW-COIN" + index, String.valueOf(index)))
                .toList());

        assertThat(service.resolve(List.of("ALL_KRW"))).hasSize(50);
        assertThat(service.resolve(List.of("ALL_KRW")).getFirst()).isEqualTo("KRW-COIN55");
    }

    @Test
    void allKrwAllowsAnyKrwMarketForRiskValidation() {
        assertThat(service.isAllowed("KRW-XRP", List.of("ALL_KRW"))).isTrue();
        assertThat(service.isAllowed("BTC-XRP", List.of("ALL_KRW"))).isFalse();
    }

    private UpbitKrwTickerResponse ticker(String market) {
        return new UpbitKrwTickerResponse(market, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN);
    }

    private UpbitKrwTickerResponse ticker(String market, String tradeAmount24h) {
        return new UpbitKrwTickerResponse(market, BigDecimal.ONE, BigDecimal.ZERO, new BigDecimal(tradeAmount24h));
    }
}
