package dev.ricciarelli.flowpay.domain.model;

public record PaymentProcessedEvent(Payment payment, boolean replayed) {
}

