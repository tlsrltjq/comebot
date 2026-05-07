package com.giseop.comebot.exchange;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ExchangeSymbolMapper {

    private ExchangeSymbolMapper() {
    }

    public static String btcMarket(ExchangeMode exchangeMode) {
        return switch (exchangeMode) {
            case UPBIT -> "KRW-BTC";
            case BINANCE -> "BTCUSDT";
        };
    }

    public static String toBinanceUsdtSymbol(String upbitKrwMarket) {
        if (upbitKrwMarket == null || !upbitKrwMarket.startsWith("KRW-")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only KRW markets can be mapped to Binance USDT symbols");
        }
        String baseAsset = upbitKrwMarket.substring("KRW-".length());
        if (baseAsset.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Market base asset must not be blank");
        }
        return baseAsset + "USDT";
    }
}
