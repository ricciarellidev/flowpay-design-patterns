package dev.ricciarelli.flowpay;

import dev.ricciarelli.flowpay.domain.model.Payment;
import dev.ricciarelli.flowpay.domain.model.PaymentMethod;
import dev.ricciarelli.flowpay.domain.model.PaymentStatus;
import dev.ricciarelli.flowpay.domain.model.ProcessPaymentCommand;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

public final class TestPayments {

    public static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");

    private TestPayments() {
    }

    public static ProcessPaymentCommand command(String amount, String currency, PaymentMethod method, String token) {
        return new ProcessPaymentCommand("customer_42", new BigDecimal(amount),
                Currency.getInstance(currency), method, token, "Sample order");
    }

    public static ProcessPaymentCommand pix() {
        return command("125.90", "BRL", PaymentMethod.PIX, null);
    }

    public static Payment payment() {
        return new Payment(UUID.randomUUID(), "customer_42", new BigDecimal("125.90"),
                Currency.getInstance("BRL"), PaymentMethod.PIX, PaymentStatus.APPROVED, "aster_sample", NOW);
    }
}
