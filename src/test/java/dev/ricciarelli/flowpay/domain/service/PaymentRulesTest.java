package dev.ricciarelli.flowpay.domain.service;

import dev.ricciarelli.flowpay.domain.model.PaymentMethod;
import dev.ricciarelli.flowpay.domain.model.ProcessPaymentCommand;
import dev.ricciarelli.flowpay.domain.validation.AmountRule;
import dev.ricciarelli.flowpay.domain.validation.CurrencyRule;
import dev.ricciarelli.flowpay.domain.validation.PaymentMethodRule;
import dev.ricciarelli.flowpay.domain.validation.PaymentRule;
import dev.ricciarelli.flowpay.domain.validation.PaymentValidationChain;
import dev.ricciarelli.flowpay.domain.validation.PaymentValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static dev.ricciarelli.flowpay.TestPayments.command;
import static dev.ricciarelli.flowpay.TestPayments.pix;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentRulesTest {

    private final AmountRule amountRule = new AmountRule(new BigDecimal("100000.00"));
    private final CurrencyRule currencyRule = new CurrencyRule(Set.of("BRL", "USD", "EUR"));
    private final PaymentMethodRule methodRule = new PaymentMethodRule();

    @ParameterizedTest
    @ValueSource(strings = {"0", "-0.01", "-100.00"})
    void rejectsNonPositiveAmounts(String amount) {
        assertThatThrownBy(() -> amountRule.validate(command(amount, "BRL", PaymentMethod.PIX, null)))
                .isInstanceOf(PaymentValidationException.class).hasMessageContaining("greater than zero");
    }

    @Test
    void rejectsNullAndExcessPrecision() {
        assertThatThrownBy(() -> amountRule.validate(new ProcessPaymentCommand("c", null, null, null, null, null)))
                .isInstanceOf(PaymentValidationException.class);
        assertThatThrownBy(() -> amountRule.validate(command("1.001", "BRL", PaymentMethod.PIX, null)))
                .isInstanceOf(PaymentValidationException.class).hasMessageContaining("two decimal");
    }

    @Test
    void acceptsTheLimitAndRejectsTheNextCent() {
        assertThatCode(() -> amountRule.validate(command("100000.00", "BRL", PaymentMethod.PIX, null)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> amountRule.validate(command("100000.01", "BRL", PaymentMethod.PIX, null)))
                .isInstanceOf(PaymentValidationException.class).hasMessageContaining("limit");
    }

    @Test
    void rejectsUnsupportedAndMissingCurrencies() {
        assertThatThrownBy(() -> currencyRule.validate(command("10", "JPY", PaymentMethod.BANK_TRANSFER, null)))
                .isInstanceOf(PaymentValidationException.class);
        assertThatThrownBy(() -> currencyRule.validate(new ProcessPaymentCommand("c", BigDecimal.ONE, null, null, null, null)))
                .isInstanceOf(PaymentValidationException.class);
    }

    @Test
    void requiresBrlForPix() {
        assertThatThrownBy(() -> methodRule.validate(command("10", "USD", PaymentMethod.PIX, null)))
                .isInstanceOf(PaymentValidationException.class).hasMessageContaining("BRL");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "card_123", "tok_", "tok_<script>"})
    void rejectsInvalidCardTokens(String token) {
        assertThatThrownBy(() -> methodRule.validate(command("10", "USD", PaymentMethod.CREDIT_CARD, token)))
                .isInstanceOf(PaymentValidationException.class);
    }

    @Test
    void rejectsMissingMethodAndToken() {
        assertThatThrownBy(() -> methodRule.validate(command("10", "BRL", null, null)))
                .isInstanceOf(PaymentValidationException.class);
        assertThatThrownBy(() -> methodRule.validate(command("10", "BRL", PaymentMethod.CREDIT_CARD, null)))
                .isInstanceOf(PaymentValidationException.class);
    }

    @Test
    void acceptsValidMethodsAndCurrencies() {
        var chain = new PaymentValidationChain(List.of(amountRule, currencyRule, methodRule));
        assertThatCode(() -> chain.validate(pix())).doesNotThrowAnyException();
        assertThatCode(() -> chain.validate(command("10", "USD", PaymentMethod.CREDIT_CARD, "tok_approved")))
                .doesNotThrowAnyException();
        assertThatCode(() -> chain.validate(command("10", "EUR", PaymentMethod.BANK_TRANSFER, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void stopsAtTheFirstFailingHandler() {
        var visited = new ArrayList<Integer>();
        PaymentRule first = command -> visited.add(1);
        PaymentRule second = command -> { throw new PaymentValidationException("TEST", "Stopped"); };
        PaymentRule last = command -> visited.add(3);

        assertThatThrownBy(() -> new PaymentValidationChain(List.of(first, second, last)).validate(pix()))
                .isInstanceOf(PaymentValidationException.class);
        assertThat(visited).containsExactly(1);
    }

    @Test
    void fingerprintNormalizesScaleButDetectsPayloadChanges() {
        var first = command("125.90", "BRL", PaymentMethod.CREDIT_CARD, "tok_approved");
        var equivalent = command("125.900", "BRL", PaymentMethod.CREDIT_CARD, "tok_approved");
        var changed = command("125.91", "BRL", PaymentMethod.CREDIT_CARD, "tok_approved");

        assertThat(first.fingerprint()).isEqualTo(equivalent.fingerprint()).isNotEqualTo(changed.fingerprint());
        assertThat(first.fingerprint()).hasSize(64).doesNotContain("tok_");
    }

    @Test
    void fingerprintDoesNotConfuseFieldBoundaries() {
        var first = new ProcessPaymentCommand("customer", BigDecimal.TEN, pix().currency(),
                PaymentMethod.CREDIT_CARD, "tok_a|b", "c");
        var second = new ProcessPaymentCommand("customer", BigDecimal.TEN, pix().currency(),
                PaymentMethod.CREDIT_CARD, "tok_a", "b|c");

        // These tuples collide with a naive pipe-joined fingerprint, but not with length prefixes.
        assertThat(first.fingerprint()).isNotEqualTo(second.fingerprint());
    }

    @Test
    void fingerprintTreatsAbsentAndEmptyOptionalValuesEqually() {
        var absent = new ProcessPaymentCommand("customer", BigDecimal.TEN, pix().currency(),
                PaymentMethod.PIX, null, null);
        var empty = new ProcessPaymentCommand(absent.customerId(), absent.amount(), absent.currency(),
                absent.method(), "", "");

        assertThat(absent.fingerprint()).isEqualTo(empty.fingerprint());
    }
}
