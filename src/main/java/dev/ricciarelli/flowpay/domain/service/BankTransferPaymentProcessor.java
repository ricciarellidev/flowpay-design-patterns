package dev.ricciarelli.flowpay.domain.service;

import dev.ricciarelli.flowpay.domain.model.GatewayResult;
import dev.ricciarelli.flowpay.domain.model.PaymentMethod;
import dev.ricciarelli.flowpay.domain.model.ProcessPaymentCommand;
import dev.ricciarelli.flowpay.domain.port.PaymentGateway;

public class BankTransferPaymentProcessor implements PaymentProcessor {

    private final PaymentGateway gateway;

    public BankTransferPaymentProcessor(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.BANK_TRANSFER;
    }

    @Override
    public GatewayResult process(ProcessPaymentCommand command) {
        return gateway.authorize(command, "bank-transfer");
    }
}

