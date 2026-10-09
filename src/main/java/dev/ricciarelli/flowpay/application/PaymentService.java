package dev.ricciarelli.flowpay.application;

import dev.ricciarelli.flowpay.domain.model.Payment;
import dev.ricciarelli.flowpay.domain.model.PaymentOutcome;
import dev.ricciarelli.flowpay.domain.model.PaymentProcessedEvent;
import dev.ricciarelli.flowpay.domain.model.ProcessPaymentCommand;
import dev.ricciarelli.flowpay.domain.port.IdempotencyStore;
import dev.ricciarelli.flowpay.domain.port.PaymentRepository;
import dev.ricciarelli.flowpay.domain.service.PaymentProcessorRegistry;
import dev.ricciarelli.flowpay.domain.validation.PaymentValidationChain;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentValidationChain validationChain;
    private final PaymentProcessorRegistry processorRegistry;
    private final PaymentRepository repository;
    private final IdempotencyStore idempotencyStore;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public PaymentService(
            PaymentValidationChain validationChain,
            PaymentProcessorRegistry processorRegistry,
            PaymentRepository repository,
            IdempotencyStore idempotencyStore,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.validationChain = validationChain;
        this.processorRegistry = processorRegistry;
        this.repository = repository;
        this.idempotencyStore = idempotencyStore;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public PaymentOutcome process(ProcessPaymentCommand command, String idempotencyKey) {
        validationChain.validate(command);

        var result = idempotencyStore.executeOnce(
                idempotencyKey,
                command.fingerprint(),
                () -> processNewPayment(command));

        var outcome = new PaymentOutcome(result.payment(), result.replayed());
        eventPublisher.publishEvent(new PaymentProcessedEvent(outcome.payment(), outcome.replayed()));
        return outcome;
    }

    public Payment findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
    }

    private Payment processNewPayment(ProcessPaymentCommand command) {
        var gatewayResult = processorRegistry.get(command.method()).process(command);
        var payment = new Payment(
                UUID.randomUUID(),
                command.customerId(),
                command.amount(),
                command.currency(),
                command.method(),
                gatewayResult.status(),
                gatewayResult.providerReference(),
                Instant.now(clock));
        return repository.save(payment);
    }
}

