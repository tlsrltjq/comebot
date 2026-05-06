package com.giseop.comebot.mvp2.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ExchangeSymbolNormalizerTest {

    private final ExchangeSymbolNormalizer normalizer = new ExchangeSymbolNormalizer();

    @Test
    void normalizesUpbitSymbol() {
        assertThat(normalizer.normalize(Exchange.UPBIT, " krw_btc ")).isEqualTo("KRW-BTC");
    }

    @Test
    void rejectsUpbitSymbolWithoutQuotePrefix() {
        assertThatThrownBy(() -> normalizer.normalize(Exchange.UPBIT, "BTC"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Upbit symbol");
    }

    @Test
    void normalizesBinanceSymbol() {
        assertThat(normalizer.normalize(Exchange.BINANCE, " btc-usdt ")).isEqualTo("BTCUSDT");
        assertThat(normalizer.normalize(Exchange.BINANCE, "eth/usdt")).isEqualTo("ETHUSDT");
    }
}
