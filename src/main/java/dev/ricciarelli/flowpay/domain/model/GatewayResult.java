package dev.ricciarelli.flowpay.domain.model;

public record GatewayResult(PaymentStatus status, String providerReference) {
}

