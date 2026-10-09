package dev.ricciarelli.flowpay.application;

import dev.ricciarelli.flowpay.domain.model.GatewayResult;
import dev.ricciarelli.flowpay.domain.model.PaymentStatus;
import dev.ricciarelli.flowpay.domain.model.PaymentProcessedEvent;
import dev.ricciarelli.flowpay.domain.port.PaymentGateway;
import dev.ricciarelli.flowpay.domain.service.PaymentProcessorRegistry;
import dev.ricciarelli.flowpay.domain.service.PixPaymentProcessor;
import dev.ricciarelli.flowpay.domain.validation.AmountRule;
import dev.ricciarelli.flowpay.domain.validation.CurrencyRule;
import dev.ricciarelli.flowpay.domain.validation.PaymentMethodRule;
import dev.ricciarelli.flowpay.domain.validation.PaymentValidationChain;
import dev.ricciarelli.flowpay.infrastructure.persistence.InMemoryIdempotencyStore;
import dev.ricciarelli.flowpay.infrastructure.persistence.InMemoryPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.ricciarelli.flowpay.TestPayments.NOW;
import static dev.ricciarelli.flowpay.TestPayments.pix;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceTest {

    private final AtomicInteger gatewayCalls = new AtomicInteger();
    private final List<Object> events = new ArrayList<>();
    private PaymentService service;

    @BeforeEach
    void setUp() {
        PaymentGateway gateway = (command, channel) -> {
            gatewayCalls.incrementAndGet();
            return new GatewayResult(PaymentStatus.APPROVED, "aster_test");
        };
        var rules = new PaymentValidationChain(List.of(
                new AmountRule(new BigDecimal("100000")),
                new CurrencyRule(Set.of("BRL", "USD", "EUR")),
                new PaymentMethodRule()));
        service = new PaymentService(rules,
                new PaymentProcessorRegistry(List.of(new PixPaymentProcessor(gateway))),
                new InMemoryPaymentRepository(), new InMemoryIdempotencyStore(),
                events::add, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void persistsTheReceiptAndPublishesAnEvent() {
        var outcome = service.process(pix(), "payment_001");

        assertThat(outcome.replayed()).isFalse();
        assertThat(outcome.payment().processedAt()).isEqualTo(NOW);
        assertThat(service.findById(outcome.payment().id())).isEqualTo(outcome.payment());
        assertThat(events).containsExactly(new PaymentProcessedEvent(outcome.payment(), false));
    }

    @Test
    void replaysWithoutCallingTheGatewayAgain() {
        var first = service.process(pix(), "payment_002");
        var replay = service.process(pix(), "payment_002");

        assertThat(replay.payment()).isEqualTo(first.payment());
        assertThat(replay.replayed()).isTrue();
        assertThat(gatewayCalls).hasValue(1);
        assertThat(events).hasSize(2).last().isEqualTo(new PaymentProcessedEvent(first.payment(), true));
    }

    @Test
    void reportsMissingPayments() {
        assertThatThrownBy(() -> service.findById(UUID.randomUUID()))
                .isInstanceOf(PaymentNotFoundException.class);
    }
}
