package com.giseop.comebot.portfolio.service;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.portfolio.PaperPortfolioProperties;
import com.giseop.comebot.portfolio.domain.PaperPortfolio;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.domain.PaperRealizedProfit;
import com.giseop.comebot.portfolio.domain.PaperTradeLog;
import com.giseop.comebot.portfolio.repository.PaperPortfolioRepository;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PaperPortfolioService {

    private static final int PRICE_SCALE = 8;

    private final PaperPortfolioRepository paperPortfolioRepository;
    private final PaperPortfolioProperties paperPortfolioProperties;

    public PaperPortfolioService(
            PaperPortfolioRepository paperPortfolioRepository,
            PaperPortfolioProperties paperPortfolioProperties
    ) {
        this.paperPortfolioRepository = paperPortfolioRepository;
        this.paperPortfolioProperties = paperPortfolioProperties;
    }

    @PostConstruct
    public void initialize() {
        for (ExchangeMode exchange : ExchangeMode.values()) {
            if (paperPortfolioRepository.getCash(exchange).compareTo(BigDecimal.ZERO) == 0) {
                paperPortfolioRepository.saveCash(exchange, paperPortfolioProperties.getInitialCash(exchange));
            }
        }
    }

    public synchronized Optional<String> validate(OrderRequest request) {
        return validate(ExchangeMode.UPBIT, request);
    }

    public synchronized Optional<String> validate(ExchangeMode exchange, OrderRequest request) {
        if (request == null || request.side() == null) {
            return Optional.empty();
        }
        if (request.side() == OrderSide.BUY && orderAmount(request).compareTo(paperPortfolioRepository.getCash(exchange)) > 0) {
            return Optional.of("Paper cash is not enough");
        }
        if (request.side() == OrderSide.SELL) {
            BigDecimal currentQuantity = paperPortfolioRepository.findPosition(exchange, request.market())
                    .map(PaperPosition::quantity)
                    .orElse(BigDecimal.ZERO);
            if (request.quantity().compareTo(currentQuantity) > 0) {
                return Optional.of("Paper position quantity is not enough");
            }
        }
        return Optional.empty();
    }

    public synchronized void apply(OrderResult result) {
        apply(ExchangeMode.UPBIT, result);
    }

    public synchronized void apply(ExchangeMode exchange, OrderResult result) {
        if (result == null || result.status() != OrderStatus.FILLED) {
            return;
        }
        if (result.side() == OrderSide.BUY) {
            applyBuy(exchange, result);
        }
        if (result.side() == OrderSide.SELL) {
            applySell(exchange, result);
        }
    }

    public PaperPortfolio getPortfolio() {
        return getPortfolio(ExchangeMode.UPBIT);
    }

    public PaperPortfolio getPortfolio(ExchangeMode exchange) {
        return paperPortfolioRepository.getPortfolio(exchange);
    }

    public List<PaperPosition> findPositions() {
        return findPositions(ExchangeMode.UPBIT);
    }

    public List<PaperPosition> findPositions(ExchangeMode exchange) {
        return paperPortfolioRepository.findPositions(exchange);
    }

    public Optional<PaperPosition> findPosition(ExchangeMode exchange, String market) {
        return paperPortfolioRepository.findPosition(exchange, market);
    }

    public BigDecimal realizedLossSince(Instant from) {
        return realizedLossSince(ExchangeMode.UPBIT, from);
    }

    public BigDecimal realizedLossSince(ExchangeMode exchange, Instant from) {
        return paperPortfolioRepository.findRealizedProfitsSince(exchange, from).stream()
                .map(PaperRealizedProfit::profit)
                .filter(profit -> profit.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void applyBuy(ExchangeMode exchange, OrderResult result) {
        BigDecimal amount = result.quantity().multiply(result.price());
        PaperPosition current = paperPortfolioRepository.findPosition(exchange, result.market())
                .orElse(new PaperPosition(result.market(), BigDecimal.ZERO, BigDecimal.ZERO));
        BigDecimal totalQuantity = current.quantity().add(result.quantity());
        BigDecimal totalCost = current.quantity().multiply(current.averageBuyPrice()).add(amount);
        BigDecimal averageBuyPrice = totalCost.divide(totalQuantity, PRICE_SCALE, RoundingMode.HALF_UP);

        paperPortfolioRepository.saveCash(exchange, paperPortfolioRepository.getCash(exchange).subtract(amount));
        paperPortfolioRepository.savePosition(exchange, new PaperPosition(result.market(), totalQuantity, averageBuyPrice));
        paperPortfolioRepository.saveTradeLog(exchange, tradeLog(result, amount, null));
    }

    private void applySell(ExchangeMode exchange, OrderResult result) {
        PaperPosition current = paperPortfolioRepository.findPosition(exchange, result.market())
                .orElse(new PaperPosition(result.market(), BigDecimal.ZERO, BigDecimal.ZERO));
        BigDecimal remainingQuantity = current.quantity().subtract(result.quantity());
        BigDecimal profit = result.price().subtract(current.averageBuyPrice()).multiply(result.quantity());

        paperPortfolioRepository.saveCash(exchange, paperPortfolioRepository.getCash(exchange).add(result.quantity().multiply(result.price())));
        paperPortfolioRepository.saveRealizedProfit(exchange, paperPortfolioRepository.getRealizedProfit(exchange).add(profit));
        paperPortfolioRepository.saveRealizedProfitEvent(exchange, new PaperRealizedProfit(profit, result.executedAt()));
        paperPortfolioRepository.savePosition(exchange, new PaperPosition(result.market(), remainingQuantity, current.averageBuyPrice()));
        paperPortfolioRepository.saveTradeLog(exchange, tradeLog(result, result.quantity().multiply(result.price()), profit));
    }

    private BigDecimal orderAmount(OrderRequest request) {
        return request.quantity().multiply(request.price());
    }

    private PaperTradeLog tradeLog(OrderResult result, BigDecimal grossAmount, BigDecimal realizedProfit) {
        return new PaperTradeLog(
                result.market(),
                result.side(),
                result.quantity(),
                result.price(),
                grossAmount,
                realizedProfit,
                result.executedAt()
        );
    }
}
