package com.giseop.comebot.portfolio.repository;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.portfolio.domain.PaperPortfolio;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.domain.PaperRealizedProfit;
import com.giseop.comebot.portfolio.domain.PaperTradeLog;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaperPortfolioRepository {

    default BigDecimal getCash() {
        return getCash(ExchangeMode.UPBIT);
    }

    BigDecimal getCash(ExchangeMode exchange);

    default void saveCash(BigDecimal cash) {
        saveCash(ExchangeMode.UPBIT, cash);
    }

    void saveCash(ExchangeMode exchange, BigDecimal cash);

    default BigDecimal getRealizedProfit() {
        return getRealizedProfit(ExchangeMode.UPBIT);
    }

    BigDecimal getRealizedProfit(ExchangeMode exchange);

    default void saveRealizedProfit(BigDecimal realizedProfit) {
        saveRealizedProfit(ExchangeMode.UPBIT, realizedProfit);
    }

    void saveRealizedProfit(ExchangeMode exchange, BigDecimal realizedProfit);

    default void saveRealizedProfitEvent(PaperRealizedProfit realizedProfit) {
        saveRealizedProfitEvent(ExchangeMode.UPBIT, realizedProfit);
    }

    void saveRealizedProfitEvent(ExchangeMode exchange, PaperRealizedProfit realizedProfit);

    default void saveTradeLog(PaperTradeLog tradeLog) {
        saveTradeLog(ExchangeMode.UPBIT, tradeLog);
    }

    void saveTradeLog(ExchangeMode exchange, PaperTradeLog tradeLog);

    default List<PaperTradeLog> findTradeLogsSince(Instant from) {
        return findTradeLogsSince(ExchangeMode.UPBIT, from);
    }

    List<PaperTradeLog> findTradeLogsSince(ExchangeMode exchange, Instant from);

    default List<PaperRealizedProfit> findRealizedProfitsSince(Instant from) {
        return findRealizedProfitsSince(ExchangeMode.UPBIT, from);
    }

    List<PaperRealizedProfit> findRealizedProfitsSince(ExchangeMode exchange, Instant from);

    default Optional<PaperPosition> findPosition(String market) {
        return findPosition(ExchangeMode.UPBIT, market);
    }

    Optional<PaperPosition> findPosition(ExchangeMode exchange, String market);

    default List<PaperPosition> findPositions() {
        return findPositions(ExchangeMode.UPBIT);
    }

    List<PaperPosition> findPositions(ExchangeMode exchange);

    default void savePosition(PaperPosition position) {
        savePosition(ExchangeMode.UPBIT, position);
    }

    void savePosition(ExchangeMode exchange, PaperPosition position);

    default PaperPortfolio getPortfolio() {
        return getPortfolio(ExchangeMode.UPBIT);
    }

    PaperPortfolio getPortfolio(ExchangeMode exchange);
}
