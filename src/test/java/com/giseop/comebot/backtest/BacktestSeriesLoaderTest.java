package com.giseop.comebot.backtest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BacktestSeriesLoaderTest {

    @Test
    void excludesUpbitKrwUsdtMarketFromBinanceUniverse() {
        assertThat(BacktestSeriesLoader.matchesExchangeName("KRW-USDT", "UPBIT")).isTrue();
        assertThat(BacktestSeriesLoader.matchesExchangeName("KRW-USDT", "BINANCE")).isFalse();
        assertThat(BacktestSeriesLoader.matchesExchangeName("BTCUSDT", "BINANCE")).isTrue();
        assertThat(BacktestSeriesLoader.matchesExchangeName("BTCUSDT", "UPBIT")).isFalse();
    }
}
