package com.giseop.comebot.market.controller;

import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.market.dto.MarketPriceResponse;
import com.giseop.comebot.market.dto.MarketPriceUpdateRequest;
import com.giseop.comebot.market.provider.InMemoryMarketPriceProvider;
import java.math.BigDecimal;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MarketPriceController {

    private final InMemoryMarketPriceProvider marketPriceProvider;

    public MarketPriceController(ObjectProvider<InMemoryMarketPriceProvider> marketPriceProvider) {
        this.marketPriceProvider = marketPriceProvider.getIfAvailable();
    }

    @GetMapping("/api/market-prices/{market}")
    public ResponseEntity<MarketPriceResponse> getPrice(@PathVariable String market) {
        if (isBlank(market) || marketPriceProvider == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(toResponse(marketPriceProvider.getCurrentPrice(market)));
    }

    @PutMapping("/api/market-prices/{market}")
    public ResponseEntity<MarketPriceResponse> updatePrice(
            @PathVariable String market,
            @RequestBody(required = false) MarketPriceUpdateRequest request
    ) {
        if (isBlank(market) || marketPriceProvider == null || request == null || isInvalidPrice(request.price())) {
            return ResponseEntity.badRequest().build();
        }

        MarketPrice updatedPrice = marketPriceProvider.updatePrice(market, request.price());
        return ResponseEntity.ok(toResponse(updatedPrice));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isInvalidPrice(BigDecimal price) {
        return price == null || price.compareTo(BigDecimal.ZERO) <= 0;
    }

    private MarketPriceResponse toResponse(MarketPrice marketPrice) {
        return new MarketPriceResponse(
                marketPrice.market(),
                marketPrice.currentPrice(),
                marketPrice.capturedAt()
        );
    }
}
