package dev.ricciarelli.flowpay.domain.validation;

import dev.ricciarelli.flowpay.domain.model.ProcessPaymentCommand;

public interface PaymentRule {
    void validate(ProcessPaymentCommand command);
}

