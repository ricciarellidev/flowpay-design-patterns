package dev.ricciarelli.flowpay.web;

import dev.ricciarelli.flowpay.application.PaymentService;
import dev.ricciarelli.flowpay.web.dto.PaymentRequest;
import dev.ricciarelli.flowpay.web.dto.PaymentResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> process(
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = "[A-Za-z0-9_-]{8,64}") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request
    ) {
        var outcome = paymentService.process(request.toCommand(), idempotencyKey);
        var response = PaymentResponse.from(outcome.payment());

        if (outcome.replayed()) {
            return ResponseEntity.ok()
                    .header("Idempotent-Replayed", "true")
                    .body(response);
        }

        return ResponseEntity.created(URI.create("/api/v1/payments/" + response.id()))
                .header("Idempotent-Replayed", "false")
                .body(response);
    }

    @GetMapping("/{id}")
    public PaymentResponse findById(@PathVariable UUID id) {
        return PaymentResponse.from(paymentService.findById(id));
    }
}

