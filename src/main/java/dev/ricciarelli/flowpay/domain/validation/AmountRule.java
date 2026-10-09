package dev.ricciarelli.flowpay.domain.validation;

import dev.ricciarelli.flowpay.domain.model.ProcessPaymentCommand;

import java.math.BigDecimal;

public class AmountRule implements PaymentRule {

    private final BigDecimal maximumAmount;

    public AmountRule(BigDecimal maximumAmount) {
        this.maximumAmount = maximumAmount;
    }

    @Override
    public void validate(ProcessPaymentCommand command) {
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("INVALID_AMOUNT", "Amount must be greater than zero.");
        }
        if (command.amount().stripTrailingZeros().scale() > 2) {
            throw new PaymentValidationException("INVALID_AMOUNT_SCALE", "Amount accepts at most two decimal places.");
        }
        if (command.amount().compareTo(maximumAmount) > 0) {
            throw new PaymentValidationException(
                    "AMOUNT_LIMIT_EXCEEDED",
                    "Amount exceeds the configured processing limit.");
        }
    }
}

