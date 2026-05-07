package com.giseop.comebot.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ExchangeSymbolMapperTest {

    @Test
    void btcMarketMapsExchangeSpecificBtcSymbol() {
        assertThat(ExchangeSymbolMapper.btcMarket(ExchangeMode.UPBIT)).isEqualTo("KRW-BTC");
        assertThat(ExchangeSymbolMapper.btcMarket(ExchangeMode.BINANCE)).isEqualTo("BTCUSDT");
    }

    @Test
    void toBinanceUsdtSymbolMapsKrwMarket() {
        assertThat(ExchangeSymbolMapper.toBinanceUsdtSymbol("KRW-BTC")).isEqualTo("BTCUSDT");
        assertThat(ExchangeSymbolMapper.toBinanceUsdtSymbol("KRW-ETH")).isEqualTo("ETHUSDT");
    }

    @Test
    void toBinanceUsdtSymbolRejectsNonKrwMarket() {
        assertThatThrownBy(() -> ExchangeSymbolMapper.toBinanceUsdtSymbol("BTC-USDT"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
