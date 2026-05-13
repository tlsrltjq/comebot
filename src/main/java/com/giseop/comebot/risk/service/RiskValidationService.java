package com.giseop.comebot.risk.service;

import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.market.service.MarketSelectionService;
import com.giseop.comebot.risk.domain.RiskCheckResult;
import com.giseop.comebot.risk.domain.RiskDecision;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RiskValidationService {

    private final TradingProperties tradingProperties;
    private final MarketSelectionService marketSelectionService;
    private final ConcentrationRiskValidationService concentrationRiskValidationService;
    private final StopLossCooldownValidationService stopLossCooldownValidationService;

    @Autowired
    public RiskValidationService(
            TradingProperties tradingProperties,
            MarketSelectionService marketSelectionService,
            ConcentrationRiskValidationService concentrationRiskValidationService,
            StopLossCooldownValidationService stopLossCooldownValidationService
    ) {
        this.tradingProperties = tradingProperties;
        this.marketSelectionService = marketSelectionService;
        this.concentrationRiskValidationService = concentrationRiskValidationService;
        this.stopLossCooldownValidationService = stopLossCooldownValidationService;
    }

    public RiskValidationService(TradingProperties tradingProperties) {
        this(
                tradingProperties,
                new MarketSelectionService(new com.giseop.comebot.market.service.UpbitKrwTickerStore()),
                null,
                null
        );
    }

    public RiskCheckResult validate(OrderRequest request) {
        return validate(ExchangeMode.UPBIT, request);
    }

    public RiskCheckResult validate(ExchangeMode exchange, OrderRequest request) {
        if (request == null) {
            return rejected("Order request must not be null");
        }
        if (request.market() == null || request.market().isBlank()) {
            return rejected("Market must not be blank");
        }
        if (request.side() == null) {
            return rejected("Order side must not be null");
        }
        if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            return rejected("Quantity must be greater than zero");
        }
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            return rejected("Price must be greater than zero");
        }
        if (exchange == ExchangeMode.BINANCE && !request.market().endsWith("USDT")) {
            return rejected("Binance PAPER orders only support USDT spot symbols");
        }
        if (exchange != ExchangeMode.BINANCE && !marketSelectionService.isAllowed(request.market(), tradingProperties.getAllowedMarkets())) {
            return rejected("Market is not allowed");
        }

        BigDecimal orderAmount = request.quantity().multiply(request.price());
        if (orderAmount.compareTo(tradingProperties.getMaxOrderAmount()) > 0) {
            return rejected("Order amount exceeds max order amount");
        }
        if (concentrationRiskValidationService != null) {
            RiskCheckResult concentrationRiskResult = concentrationRiskValidationService.validate(exchange, request);
            if (concentrationRiskResult.decision() == RiskDecision.REJECTED) {
                return concentrationRiskResult;
            }
        }
        if (stopLossCooldownValidationService != null) {
            RiskCheckResult stopLossCooldownResult = stopLossCooldownValidationService.validate(exchange, request);
            if (stopLossCooldownResult.decision() == RiskDecision.REJECTED) {
                return stopLossCooldownResult;
            }
        }

        return new RiskCheckResult(RiskDecision.APPROVED, "Risk check approved", Instant.now());
    }

    private RiskCheckResult rejected(String reason) {
        return new RiskCheckResult(RiskDecision.REJECTED, reason, Instant.now());
    }
}
