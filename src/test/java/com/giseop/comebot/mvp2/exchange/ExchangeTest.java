package com.giseop.comebot.mvp2.exchange;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExchangeTest {

    @Test
    void mvp2ExchangesUsePublicMarketDataOnly() {
        assertThat(Exchange.values())
                .extracting(Exchange::isPublicMarketDataOnly)
                .containsOnly(true);
    }

    @Test
    void mvp2StartsWithUpbitAndBinance() {
        assertThat(Exchange.values())
                .containsExactly(Exchange.UPBIT, Exchange.BINANCE);
    }
}
