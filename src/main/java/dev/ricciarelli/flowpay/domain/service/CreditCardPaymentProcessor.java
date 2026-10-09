package dev.ricciarelli.flowpay.domain.service;

import dev.ricciarelli.flowpay.domain.model.GatewayResult;
import dev.ricciarelli.flowpay.domain.model.PaymentMethod;
import dev.ricciarelli.flowpay.domain.model.ProcessPaymentCommand;
import dev.ricciarelli.flowpay.domain.port.PaymentGateway;

public class CreditCardPaymentProcessor implements PaymentProcessor {

    private final PaymentGateway gateway;

    public CreditCardPaymentProcessor(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.CREDIT_CARD;
    }

    @Override
    public GatewayResult process(ProcessPaymentCommand command) {
        return gateway.authorize(command, "card-token");
    }
}

