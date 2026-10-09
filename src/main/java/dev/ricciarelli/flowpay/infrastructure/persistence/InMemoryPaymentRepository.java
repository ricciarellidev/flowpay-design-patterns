package dev.ricciarelli.flowpay.infrastructure.persistence;

import dev.ricciarelli.flowpay.domain.model.Payment;
import dev.ricciarelli.flowpay.domain.port.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final ConcurrentHashMap<UUID, Payment> payments = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        payments.put(payment.id(), payment);
        return payment;
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return Optional.ofNullable(payments.get(id));
    }
}

