package dev.ricciarelli.flowpay.domain.validation;

import dev.ricciarelli.flowpay.domain.model.ProcessPaymentCommand;

import java.util.List;

public class PaymentValidationChain {

    private final List<PaymentRule> rules;

    public PaymentValidationChain(List<PaymentRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public void validate(ProcessPaymentCommand command) {
        rules.forEach(rule -> rule.validate(command));
    }
}

