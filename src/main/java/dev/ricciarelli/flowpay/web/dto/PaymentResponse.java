package dev.ricciarelli.flowpay.web.dto;

import dev.ricciarelli.flowpay.domain.model.Payment;
import dev.ricciarelli.flowpay.domain.model.PaymentMethod;
import dev.ricciarelli.flowpay.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String customerId,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        PaymentStatus status,
        String providerReference,
        Instant processedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.id(),
                payment.customerId(),
                payment.amount(),
                payment.currency().getCurrencyCode(),
                payment.method(),
                payment.status(),
                payment.providerReference(),
                payment.processedAt());
    }
}

