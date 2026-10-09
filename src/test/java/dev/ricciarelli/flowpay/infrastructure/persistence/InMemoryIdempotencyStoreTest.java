package dev.ricciarelli.flowpay.infrastructure.persistence;

import dev.ricciarelli.flowpay.application.IdempotencyConflictException;
import dev.ricciarelli.flowpay.domain.port.IdempotencyStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.ricciarelli.flowpay.TestPayments.payment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryIdempotencyStoreTest {

    @Test
    void runsOnceAndRejectsAChangedPayload() {
        var store = new InMemoryIdempotencyStore();
        var receipt = payment();
        var first = store.executeOnce("key_001", "digest_a", () -> receipt);
        var replay = store.executeOnce("key_001", "digest_a", () -> { throw new AssertionError("Duplicate call"); });

        assertThat(first.replayed()).isFalse();
        assertThat(replay.replayed()).isTrue();
        assertThat(replay.payment()).isEqualTo(receipt);
        assertThatThrownBy(() -> store.executeOnce("key_001", "digest_b", () -> receipt))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void allowsARetryAfterAPreAuthorizationFailure() {
        var store = new InMemoryIdempotencyStore();
        assertThatThrownBy(() -> store.executeOnce("key_002", "digest_a", () -> {
            throw new IllegalStateException("Simulated pre-authorization failure");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(store.executeOnce("key_002", "digest_a", InMemoryIdempotencyStoreTest::sample).replayed())
                .isFalse();
    }

    @Test
    void releasesTheReservationAfterAnError() {
        var store = new InMemoryIdempotencyStore();
        assertThatThrownBy(() -> store.executeOnce("key_error", "digest", () -> {
            throw new AssertionError("Simulated error");
        })).isInstanceOf(AssertionError.class);
        assertThat(store.executeOnce("key_error", "digest", InMemoryIdempotencyStoreTest::sample).replayed())
                .isFalse();
    }

    @Test
    void deduplicatesConcurrentRequests() throws Exception {
        var store = new InMemoryIdempotencyStore();
        var calls = new AtomicInteger();
        var receipt = payment();
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(12);
        var futures = new ArrayList<Future<IdempotencyStore.IdempotentResult>>();

        try {
            for (int index = 0; index < 24; index++) {
                futures.add(pool.submit(() -> {
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Test synchronization timed out");
                    }
                    return store.executeOnce("concurrent_key", "same_digest", () -> {
                        calls.incrementAndGet();
                        return receipt;
                    });
                }));
            }
            start.countDown();

            int newResponses = 0;
            for (var future : futures) {
                var outcome = future.get(5, TimeUnit.SECONDS);
                assertThat(outcome.payment()).isEqualTo(receipt);
                if (!outcome.replayed()) {
                    newResponses++;
                }
            }
            assertThat(calls).hasValue(1);
            assertThat(newResponses).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private static dev.ricciarelli.flowpay.domain.model.Payment sample() {
        return payment();
    }
}
