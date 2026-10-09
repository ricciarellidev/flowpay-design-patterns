package dev.ricciarelli.flowpay.domain.service;

import dev.ricciarelli.flowpay.domain.model.PaymentMethod;

public class UnsupportedPaymentMethodException extends RuntimeException {

    public UnsupportedPaymentMethodException(PaymentMethod method) {
        super("No processor registered for payment method: " + method);
    }
}

