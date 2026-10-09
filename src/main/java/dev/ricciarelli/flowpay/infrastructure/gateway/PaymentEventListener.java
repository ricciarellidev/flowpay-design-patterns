package dev.ricciarelli.flowpay.infrastructure.gateway;

import dev.ricciarelli.flowpay.domain.model.PaymentProcessedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentEventListener.class);

    private final MeterRegistry meterRegistry;

    public PaymentEventListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @EventListener
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        var payment = event.payment();
        LOGGER.info(
                "payment_processed id={} method={} status={} replayed={}",
                payment.id(), payment.method(), payment.status(), event.replayed());

        meterRegistry.counter(
                        "flowpay.payments.processed",
                        "method", payment.method().name(),
                        "status", payment.status().name(),
                        "replayed", Boolean.toString(event.replayed()))
                .increment();
    }
}

