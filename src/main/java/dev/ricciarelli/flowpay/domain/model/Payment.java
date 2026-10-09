package dev.ricciarelli.flowpay.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

public record Payment(
        UUID id,
        String customerId,
        BigDecimal amount,
        Currency currency,
        PaymentMethod method,
        PaymentStatus status,
        String providerReference,
        Instant processedAt
) {
    public Payment {
        Objects.requireNonNull(id);
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);
        Objects.requireNonNull(method);
        Objects.requireNonNull(status);
        Objects.requireNonNull(providerReference);
        Objects.requireNonNull(processedAt);
    }
}

