package com.giseop.comebot.portfolio.service;

import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.portfolio.PaperPortfolioProperties;
import com.giseop.comebot.portfolio.domain.PaperPortfolio;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.domain.PaperRealizedProfit;
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
        if (paperPortfolioRepository.getCash().compareTo(BigDecimal.ZERO) == 0) {
            paperPortfolioRepository.saveCash(paperPortfolioProperties.getInitialCash());
        }
    }

    public synchronized Optional<String> validate(OrderRequest request) {
        if (request == null || request.side() == null) {
            return Optional.empty();
        }
        if (request.side() == OrderSide.BUY && orderAmount(request).compareTo(paperPortfolioRepository.getCash()) > 0) {
            return Optional.of("Paper cash is not enough");
        }
        if (request.side() == OrderSide.SELL) {
            BigDecimal currentQuantity = paperPortfolioRepository.findPosition(request.market())
                    .map(PaperPosition::quantity)
                    .orElse(BigDecimal.ZERO);
            if (request.quantity().compareTo(currentQuantity) > 0) {
                return Optional.of("Paper position quantity is not enough");
            }
        }
        return Optional.empty();
    }

    public synchronized void apply(OrderResult result) {
        if (result == null || result.status() != OrderStatus.FILLED) {
            return;
        }
        if (result.side() == OrderSide.BUY) {
            applyBuy(result);
        }
        if (result.side() == OrderSide.SELL) {
            applySell(result);
        }
    }

    public PaperPortfolio getPortfolio() {
        return paperPortfolioRepository.getPortfolio();
    }

    public List<PaperPosition> findPositions() {
        return paperPortfolioRepository.findPositions();
    }

    public BigDecimal realizedLossSince(Instant from) {
        return paperPortfolioRepository.findRealizedProfitsSince(from).stream()
                .map(PaperRealizedProfit::profit)
                .filter(profit -> profit.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void applyBuy(OrderResult result) {
        BigDecimal amount = result.quantity().multiply(result.price());
        PaperPosition current = paperPortfolioRepository.findPosition(result.market())
                .orElse(new PaperPosition(result.market(), BigDecimal.ZERO, BigDecimal.ZERO));
        BigDecimal totalQuantity = current.quantity().add(result.quantity());
        BigDecimal totalCost = current.quantity().multiply(current.averageBuyPrice()).add(amount);
        BigDecimal averageBuyPrice = totalCost.divide(totalQuantity, PRICE_SCALE, RoundingMode.HALF_UP);

        paperPortfolioRepository.saveCash(paperPortfolioRepository.getCash().subtract(amount));
        paperPortfolioRepository.savePosition(new PaperPosition(result.market(), totalQuantity, averageBuyPrice));
    }

    private void applySell(OrderResult result) {
        PaperPosition current = paperPortfolioRepository.findPosition(result.market())
                .orElse(new PaperPosition(result.market(), BigDecimal.ZERO, BigDecimal.ZERO));
        BigDecimal remainingQuantity = current.quantity().subtract(result.quantity());
        BigDecimal profit = result.price().subtract(current.averageBuyPrice()).multiply(result.quantity());

        paperPortfolioRepository.saveCash(paperPortfolioRepository.getCash().add(result.quantity().multiply(result.price())));
        paperPortfolioRepository.saveRealizedProfit(paperPortfolioRepository.getRealizedProfit().add(profit));
        paperPortfolioRepository.saveRealizedProfitEvent(new PaperRealizedProfit(profit, result.executedAt()));
        paperPortfolioRepository.savePosition(new PaperPosition(result.market(), remainingQuantity, current.averageBuyPrice()));
    }

    private BigDecimal orderAmount(OrderRequest request) {
        return request.quantity().multiply(request.price());
    }
}
