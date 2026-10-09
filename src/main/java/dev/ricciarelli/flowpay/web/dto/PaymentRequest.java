package dev.ricciarelli.flowpay.web.dto;

import dev.ricciarelli.flowpay.domain.model.PaymentMethod;
import dev.ricciarelli.flowpay.domain.model.ProcessPaymentCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;

public record PaymentRequest(
        @NotBlank @Size(max = 80) @Pattern(regexp = "[A-Za-z0-9_-]+") String customerId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @NotNull PaymentMethod method,
        @Size(max = 120) String paymentInstrumentToken,
        @Size(max = 200) String description
) {
    public ProcessPaymentCommand toCommand() {
        return new ProcessPaymentCommand(
                customerId,
                amount,
                Currency.getInstance(currency.toUpperCase(Locale.ROOT)),
                method,
                paymentInstrumentToken,
                description);
    }
}

