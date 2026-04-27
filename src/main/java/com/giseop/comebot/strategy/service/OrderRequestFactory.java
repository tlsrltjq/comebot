package com.giseop.comebot.strategy.service;

import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.strategy.domain.TradingSignal;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OrderRequestFactory {

    public Optional<OrderRequest> create(TradingSignal signal) {
        if (signal == null || signal.signalType() == null || signal.signalType() == SignalType.HOLD) {
            return Optional.empty();
        }

        return Optional.of(new OrderRequest(
                signal.market(),
                toOrderSide(signal.signalType()),
                signal.quantity(),
                signal.targetPrice(),
                signal.detectedAt()
        ));
    }

    private OrderSide toOrderSide(SignalType signalType) {
        return switch (signalType) {
            case BUY -> OrderSide.BUY;
            case SELL -> OrderSide.SELL;
            case HOLD -> throw new IllegalArgumentException("HOLD signal must not create an order request");
        };
    }
}
