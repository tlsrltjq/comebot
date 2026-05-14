package com.giseop.comebot.execution.gateway;

import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderResult;

public interface ExecutionGateway {

    boolean supportsOnlyPaperTrading();

    OrderResult execute(OrderRequest request);
}
