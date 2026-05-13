package com.giseop.comebot.portfolio.persistence;

import com.giseop.comebot.exchange.ExchangeMode;
import java.io.Serializable;
import java.util.Objects;

public class PaperPositionId implements Serializable {

    private ExchangeMode exchange;
    private String market;

    protected PaperPositionId() {
    }

    public PaperPositionId(ExchangeMode exchange, String market) {
        this.exchange = exchange;
        this.market = market;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PaperPositionId that)) {
            return false;
        }
        return exchange == that.exchange && Objects.equals(market, that.market);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exchange, market);
    }
}
