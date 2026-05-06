package com.giseop.comebot.mvp2.exchange.controller;

import com.giseop.comebot.mvp2.exchange.Exchange;
import com.giseop.comebot.mvp2.exchange.ExchangeMarketDataProvider;
import com.giseop.comebot.mvp2.exchange.dto.Mvp2ExchangeResponse;
import com.giseop.comebot.mvp2.exchange.dto.Mvp2ExchangeStatusResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class Mvp2ExchangeController {

    private final List<ExchangeMarketDataProvider> marketDataProviders;

    public Mvp2ExchangeController(List<ExchangeMarketDataProvider> marketDataProviders) {
        this.marketDataProviders = marketDataProviders;
    }

    @GetMapping("/api/mvp2/exchanges")
    public List<Mvp2ExchangeResponse> getExchanges() {
        return marketDataProviders.stream()
                .map(ExchangeMarketDataProvider::exchange)
                .distinct()
                .sorted(Comparator.comparing(Exchange::name))
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/api/mvp2/exchanges/{exchange}/status")
    public Mvp2ExchangeStatusResponse getExchangeStatus(@PathVariable String exchange) {
        Exchange parsedExchange = parseExchange(exchange);
        if (!isEnabled(parsedExchange)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MVP2 exchange is not enabled: " + exchange);
        }
        return new Mvp2ExchangeStatusResponse(
                parsedExchange,
                parsedExchange.getDisplayName(),
                true,
                parsedExchange.isPublicMarketDataOnly(),
                false,
                marketDataMessage(parsedExchange),
                "MVP2 uses public market data only. Orders remain PAPER/SIMULATION only."
        );
    }

    private Mvp2ExchangeResponse toResponse(Exchange exchange) {
        return new Mvp2ExchangeResponse(
                exchange,
                exchange.getDisplayName(),
                true,
                exchange.isPublicMarketDataOnly(),
                "/api/mvp2/exchanges/" + exchange.name() + "/status"
        );
    }

    private boolean isEnabled(Exchange exchange) {
        return marketDataProviders.stream()
                .map(ExchangeMarketDataProvider::exchange)
                .anyMatch(enabledExchange -> enabledExchange == exchange);
    }

    private Exchange parseExchange(String exchange) {
        try {
            return Exchange.valueOf(exchange.trim().toUpperCase());
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown MVP2 exchange: " + exchange, exception);
        }
    }

    private String marketDataMessage(Exchange exchange) {
        return switch (exchange) {
            case UPBIT -> "Upbit public ticker/candle adapter is available.";
            case BINANCE -> "Binance public ticker/kline provider is available.";
        };
    }
}
