package dev.ricciarelli.flowpay.domain.model;

public record PaymentOutcome(Payment payment, boolean replayed) {
}

