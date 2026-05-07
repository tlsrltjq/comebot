package com.giseop.comebot.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ExchangeModeResolverTest {

    @Test
    void resolveDefaultsToUpbitWhenValueIsMissing() {
        assertThat(ExchangeModeResolver.resolve(null)).isEqualTo(ExchangeMode.UPBIT);
        assertThat(ExchangeModeResolver.resolve(" ")).isEqualTo(ExchangeMode.UPBIT);
    }

    @Test
    void resolveAcceptsLowercaseValues() {
        assertThat(ExchangeModeResolver.resolve("upbit")).isEqualTo(ExchangeMode.UPBIT);
        assertThat(ExchangeModeResolver.resolve("binance")).isEqualTo(ExchangeMode.BINANCE);
    }

    @Test
    void resolveRejectsUnknownExchange() {
        assertThatThrownBy(() -> ExchangeModeResolver.resolve("coinbase"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void requireImplementedRejectsBinanceForStageTwo() {
        assertThatThrownBy(() -> ExchangeModeResolver.requireImplemented(ExchangeMode.BINANCE))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
    }
}
