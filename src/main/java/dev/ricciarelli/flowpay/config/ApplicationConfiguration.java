package dev.ricciarelli.flowpay.config;

import dev.ricciarelli.flowpay.domain.port.PaymentGateway;
import dev.ricciarelli.flowpay.domain.service.BankTransferPaymentProcessor;
import dev.ricciarelli.flowpay.domain.service.CreditCardPaymentProcessor;
import dev.ricciarelli.flowpay.domain.service.PaymentProcessorRegistry;
import dev.ricciarelli.flowpay.domain.service.PixPaymentProcessor;
import dev.ricciarelli.flowpay.domain.validation.AmountRule;
import dev.ricciarelli.flowpay.domain.validation.CurrencyRule;
import dev.ricciarelli.flowpay.domain.validation.PaymentMethodRule;
import dev.ricciarelli.flowpay.domain.validation.PaymentValidationChain;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

@Configuration
@EnableConfigurationProperties(FlowPayProperties.class)
public class ApplicationConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PaymentValidationChain paymentValidationChain(FlowPayProperties properties) {
        return new PaymentValidationChain(List.of(
                new AmountRule(properties.maxAmount()),
                new CurrencyRule(properties.supportedCurrencies()),
                new PaymentMethodRule()));
    }

    @Bean
    PaymentProcessorRegistry paymentProcessorRegistry(PaymentGateway gateway) {
        return new PaymentProcessorRegistry(List.of(
                new PixPaymentProcessor(gateway),
                new CreditCardPaymentProcessor(gateway),
                new BankTransferPaymentProcessor(gateway)));
    }
}

