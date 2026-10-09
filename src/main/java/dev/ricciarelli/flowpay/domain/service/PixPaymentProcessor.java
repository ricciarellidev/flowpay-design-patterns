package dev.ricciarelli.flowpay.domain.service;

import dev.ricciarelli.flowpay.domain.model.GatewayResult;
import dev.ricciarelli.flowpay.domain.model.PaymentMethod;
import dev.ricciarelli.flowpay.domain.model.ProcessPaymentCommand;
import dev.ricciarelli.flowpay.domain.port.PaymentGateway;

public class PixPaymentProcessor implements PaymentProcessor {

    private final PaymentGateway gateway;

    public PixPaymentProcessor(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.PIX;
    }

    @Override
    public GatewayResult process(ProcessPaymentCommand command) {
        return gateway.authorize(command, "instant-payment");
    }
}

