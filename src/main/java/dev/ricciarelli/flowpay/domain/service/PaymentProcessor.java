package dev.ricciarelli.flowpay.domain.service;

import dev.ricciarelli.flowpay.domain.model.GatewayResult;
import dev.ricciarelli.flowpay.domain.model.PaymentMethod;
import dev.ricciarelli.flowpay.domain.model.ProcessPaymentCommand;

public interface PaymentProcessor {
    PaymentMethod supportedMethod();

    GatewayResult process(ProcessPaymentCommand command);
}

