package dev.ricciarelli.flowpay.domain.service;

import dev.ricciarelli.flowpay.domain.model.PaymentMethod;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class PaymentProcessorRegistry {

    private final Map<PaymentMethod, PaymentProcessor> processors;

    public PaymentProcessorRegistry(List<PaymentProcessor> processors) {
        var registry = new EnumMap<PaymentMethod, PaymentProcessor>(PaymentMethod.class);
        processors.forEach(processor -> {
            var previous = registry.put(processor.supportedMethod(), processor);
            if (previous != null) {
                throw new IllegalStateException(
                        "Multiple processors registered for " + processor.supportedMethod());
            }
        });
        this.processors = Map.copyOf(registry);
    }

    public PaymentProcessor get(PaymentMethod method) {
        var processor = processors.get(method);
        if (processor == null) {
            throw new UnsupportedPaymentMethodException(method);
        }
        return processor;
    }
}

