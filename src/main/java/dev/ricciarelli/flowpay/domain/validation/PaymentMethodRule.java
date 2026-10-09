package dev.ricciarelli.flowpay.domain.validation;

import dev.ricciarelli.flowpay.domain.model.PaymentMethod;
import dev.ricciarelli.flowpay.domain.model.ProcessPaymentCommand;

public class PaymentMethodRule implements PaymentRule {

    @Override
    public void validate(ProcessPaymentCommand command) {
        if (command.method() == null) {
            throw new PaymentValidationException("PAYMENT_METHOD_REQUIRED", "Payment method is required.");
        }
        if (command.method() == PaymentMethod.PIX
                && !"BRL".equals(command.currency().getCurrencyCode())) {
            throw new PaymentValidationException("PIX_REQUIRES_BRL", "PIX payments require BRL currency.");
        }
        if (command.method() == PaymentMethod.CREDIT_CARD
                && (command.paymentInstrumentToken() == null
                || !command.paymentInstrumentToken().matches("tok_[A-Za-z0-9_-]{1,116}"))) {
            throw new PaymentValidationException(
                    "CARD_TOKEN_REQUIRED",
                    "Credit card payments require a tokenized payment instrument.");
        }
    }
}

