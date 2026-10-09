package dev.ricciarelli.flowpay.domain.validation;

public class PaymentValidationException extends RuntimeException {

    private final String code;

    public PaymentValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}

