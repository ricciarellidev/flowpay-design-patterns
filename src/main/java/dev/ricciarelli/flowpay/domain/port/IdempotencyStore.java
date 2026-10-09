package dev.ricciarelli.flowpay.domain.port;

import dev.ricciarelli.flowpay.domain.model.Payment;
import java.util.function.Supplier;

public interface IdempotencyStore {
    IdempotentResult executeOnce(String key, String fingerprint, Supplier<Payment> operation);

    record IdempotentResult(Payment payment, boolean replayed) {
    }
}
