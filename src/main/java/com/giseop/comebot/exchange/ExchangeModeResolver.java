package com.giseop.comebot.exchange;

import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ExchangeModeResolver {

    private static final ExchangeMode DEFAULT_EXCHANGE = ExchangeMode.UPBIT;

    private ExchangeModeResolver() {
    }

    public static ExchangeMode resolve(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_EXCHANGE;
        }

        try {
            return ExchangeMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported exchange: " + value);
        }
    }

    public static void requireImplemented(ExchangeMode exchangeMode) {
        if (exchangeMode == ExchangeMode.BINANCE) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Binance exchange mode is not implemented yet");
        }
    }
}
