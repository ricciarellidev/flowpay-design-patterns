package dev.ricciarelli.flowpay.domain.model;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Currency;
import java.util.HexFormat;
import java.util.List;

public record ProcessPaymentCommand(
        String customerId,
        BigDecimal amount,
        Currency currency,
        PaymentMethod method,
        String paymentInstrumentToken,
        String description
) {
    public String fingerprint() {
        var values = List.of(
                customerId,
                amount.stripTrailingZeros().toPlainString(),
                currency.getCurrencyCode(),
                method.name(),
                paymentInstrumentToken == null ? "" : paymentInstrumentToken,
                description == null ? "" : description);
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            for (var value : values) {
                var bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime.", exception);
        }
    }
}

