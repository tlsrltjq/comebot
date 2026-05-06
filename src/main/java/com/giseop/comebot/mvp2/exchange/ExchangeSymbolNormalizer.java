package com.giseop.comebot.mvp2.exchange;

import org.springframework.stereotype.Component;

@Component
public class ExchangeSymbolNormalizer {

    public String normalize(Exchange exchange, String symbol) {
        if (exchange == null) {
            throw new IllegalArgumentException("exchange must not be null");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }

        String normalized = symbol.trim().toUpperCase();
        return switch (exchange) {
            case UPBIT -> normalizeUpbit(normalized);
            case BINANCE -> normalizeBinance(normalized);
        };
    }

    private String normalizeUpbit(String symbol) {
        String normalized = symbol.replace('_', '-');
        if (!normalized.contains("-")) {
            throw new IllegalArgumentException("Upbit symbol must include quote prefix, for example KRW-BTC");
        }
        return normalized;
    }

    private String normalizeBinance(String symbol) {
        return symbol.replace("/", "").replace("-", "").replace("_", "");
    }
}
