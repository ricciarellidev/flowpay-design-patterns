package dev.ricciarelli.flowpay.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Set;

@Validated
@ConfigurationProperties(prefix = "flowpay")
public record FlowPayProperties(
        @NotNull @DecimalMin("0.01") BigDecimal maxAmount,
        @NotEmpty Set<String> supportedCurrencies
) {
}

