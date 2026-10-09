package dev.ricciarelli.flowpay.infrastructure.persistence;

import dev.ricciarelli.flowpay.application.IdempotencyConflictException;
import dev.ricciarelli.flowpay.domain.model.Payment;
import dev.ricciarelli.flowpay.domain.port.IdempotencyStore;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final ConcurrentHashMap<String, InFlightOperation> operations = new ConcurrentHashMap<>();

    @Override
    public IdempotentResult executeOnce(
            String key,
            String fingerprint,
            Supplier<Payment> operation
    ) {
        var candidate = new InFlightOperation(fingerprint, new CompletableFuture<>());
        var existing = operations.putIfAbsent(key, candidate);

        if (existing != null) {
            if (!existing.fingerprint().equals(fingerprint)) {
                throw new IdempotencyConflictException();
            }
            return new IdempotentResult(join(existing.result()), true);
        }

        try {
            var value = operation.get();
            candidate.result().complete(value);
            return new IdempotentResult(value, false);
        } catch (RuntimeException | Error exception) {
            candidate.result().completeExceptionally(exception);
            operations.remove(key, candidate);
            throw exception;
        }
    }

    private static Payment join(CompletableFuture<Payment> future) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (exception.getCause() instanceof Error error) {
                throw error;
            }
            throw exception;
        }
    }

    private record InFlightOperation(String fingerprint, CompletableFuture<Payment> result) {
    }
}

