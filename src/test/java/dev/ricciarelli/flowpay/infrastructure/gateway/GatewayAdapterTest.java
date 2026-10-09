package dev.ricciarelli.flowpay.infrastructure.gateway;

import dev.ricciarelli.flowpay.domain.model.PaymentMethod;
import dev.ricciarelli.flowpay.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;

import static dev.ricciarelli.flowpay.TestPayments.command;
import static org.assertj.core.api.Assertions.assertThat;

class GatewayAdapterTest {

    @Test
    void convertsMoneyToMinorUnitsAndMapsApproval() {
        var captured = new AsterPayClient.AsterCharge[1];
        var client = new AsterPayClient() {
            @Override
            public AsterAuthorization authorize(AsterCharge charge) {
                captured[0] = charge;
                return new AsterAuthorization("provider_42", "AUTHORIZED");
            }
        };
        var result = new AsterPayGatewayAdapter(client)
                .authorize(command("125.90", "USD", PaymentMethod.CREDIT_CARD, "tok_approved"), "card-token");

        assertThat(captured[0].amountInMinorUnits()).isEqualTo(12590);
        assertThat(captured[0].currency()).isEqualTo("USD");
        assertThat(captured[0].channel()).isEqualTo("card-token");
        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.providerReference()).isEqualTo("provider_42");
    }

    @Test
    void mapsDeclinesFromTheSimulatedProvider() {
        var adapter = new AsterPayGatewayAdapter(new AsterPayClient());
        var result = adapter.authorize(command("10", "BRL", PaymentMethod.CREDIT_CARD, "tok_declined"), "card-token");
        assertThat(result.status()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(result.providerReference()).startsWith("aster_");
    }
}
