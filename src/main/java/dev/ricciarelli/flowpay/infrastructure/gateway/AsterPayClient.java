package dev.ricciarelli.flowpay.infrastructure.gateway;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Small stand-in for a provider SDK. The adapter keeps this provider-specific
 * model outside the domain layer.
 */
@Component
public class AsterPayClient {

    public AsterAuthorization authorize(AsterCharge charge) {
        var declined = "tok_declined".equals(charge.instrumentToken());
        var status = declined ? "DECLINED" : "AUTHORIZED";
        return new AsterAuthorization("aster_" + UUID.randomUUID(), status);
    }

    public record AsterCharge(
            String merchantCustomer,
            long amountInMinorUnits,
            String currency,
            String channel,
            String instrumentToken
    ) {
    }

    public record AsterAuthorization(String authorizationId, String status) {
    }
}

