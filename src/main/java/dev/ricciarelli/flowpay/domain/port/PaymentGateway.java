package dev.ricciarelli.flowpay.domain.port;

import dev.ricciarelli.flowpay.domain.model.GatewayResult;
import dev.ricciarelli.flowpay.domain.model.ProcessPaymentCommand;

public interface PaymentGateway {
    GatewayResult authorize(ProcessPaymentCommand command, String channel);
}

