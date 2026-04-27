package com.giseop.comebot.strategy.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.strategy.domain.TradingSignal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OrderRequestFactoryTest {

    private final OrderRequestFactory factory = new OrderRequestFactory();

    @Test
    void createReturnsOrderRequestForBuySignal() {
        TradingSignal signal = new TradingSignal(
                "KRW-BTC",
                SignalType.BUY,
                "Test buy",
                new BigDecimal("100"),
                new BigDecimal("0.5"),
                Instant.now()
        );

        Optional<OrderRequest> request = factory.create(signal);

        assertThat(request).isPresent();
        assertThat(request.get().market()).isEqualTo("KRW-BTC");
        assertThat(request.get().side()).isEqualTo(OrderSide.BUY);
        assertThat(request.get().price()).isEqualByComparingTo("100");
        assertThat(request.get().quantity()).isEqualByComparingTo("0.5");
    }

    @Test
    void createReturnsEmptyForHoldSignal() {
        TradingSignal signal = new TradingSignal(
                "KRW-BTC",
                SignalType.HOLD,
                "No action",
                new BigDecimal("150"),
                BigDecimal.ZERO,
                Instant.now()
        );

        Optional<OrderRequest> request = factory.create(signal);

        assertThat(request).isEmpty();
    }

    @Test
    void createReturnsEmptyForNullSignalType() {
        TradingSignal signal = new TradingSignal(
                "KRW-BTC",
                null,
                "Invalid signal",
                new BigDecimal("150"),
                new BigDecimal("0.5"),
                Instant.now()
        );

        Optional<OrderRequest> request = factory.create(signal);

        assertThat(request).isEmpty();
    }
}
