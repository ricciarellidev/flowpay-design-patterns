package dev.ricciarelli.flowpay.domain.service;

import dev.ricciarelli.flowpay.domain.model.GatewayResult;
import dev.ricciarelli.flowpay.domain.model.PaymentMethod;
import dev.ricciarelli.flowpay.domain.model.PaymentStatus;
import dev.ricciarelli.flowpay.domain.port.PaymentGateway;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.ricciarelli.flowpay.TestPayments.command;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentProcessorRegistryTest {

    @Test
    void routesEveryMethodToItsOwnStrategy() {
        var channels = new ArrayList<String>();
        PaymentGateway gateway = (command, channel) -> {
            channels.add(channel);
            return new GatewayResult(PaymentStatus.APPROVED, "aster_test");
        };
        var registry = new PaymentProcessorRegistry(List.of(
                new PixPaymentProcessor(gateway),
                new CreditCardPaymentProcessor(gateway),
                new BankTransferPaymentProcessor(gateway)));

        for (var method : PaymentMethod.values()) {
            var result = registry.get(method).process(command("10.00", "BRL", method, "tok_approved"));
            assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        }

        assertThat(channels).containsExactly("instant-payment", "card-token", "bank-transfer");
    }

    @Test
    void failsFastWhenTwoStrategiesClaimTheSameMethod() {
        PaymentGateway gateway = (command, channel) -> new GatewayResult(PaymentStatus.APPROVED, "aster_test");
        assertThatThrownBy(() -> new PaymentProcessorRegistry(List.of(
                new PixPaymentProcessor(gateway), new PixPaymentProcessor(gateway))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("PIX");
    }

    @Test
    void rejectsAnUnregisteredMethod() {
        assertThatThrownBy(() -> new PaymentProcessorRegistry(List.of()).get(PaymentMethod.PIX))
                .isInstanceOf(UnsupportedPaymentMethodException.class);
    }
}
