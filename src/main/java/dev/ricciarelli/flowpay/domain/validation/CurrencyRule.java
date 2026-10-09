package dev.ricciarelli.flowpay.domain.validation;

import dev.ricciarelli.flowpay.domain.model.ProcessPaymentCommand;
import java.util.Set;

public class CurrencyRule implements PaymentRule {

    private final Set<String> supportedCurrencies;

    public CurrencyRule(Set<String> supportedCurrencies) {
        this.supportedCurrencies = Set.copyOf(supportedCurrencies);
    }

    @Override
    public void validate(ProcessPaymentCommand command) {
        if (command.currency() == null
                || !supportedCurrencies.contains(command.currency().getCurrencyCode())) {
            throw new PaymentValidationException(
                    "UNSUPPORTED_CURRENCY",
                    "Currency is not supported by this service.");
        }
    }
}

