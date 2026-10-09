package dev.ricciarelli.flowpay.web.error;

import dev.ricciarelli.flowpay.application.IdempotencyConflictException;
import dev.ricciarelli.flowpay.application.PaymentNotFoundException;
import dev.ricciarelli.flowpay.domain.validation.PaymentValidationException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(PaymentNotFoundException.class)
    ResponseEntity<Object> handleNotFound(PaymentNotFoundException exception, WebRequest request) {
        return problem(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<Object> handleConflict(IdempotencyConflictException exception, WebRequest request) {
        return problem(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", exception.getMessage(), request);
    }

    @ExceptionHandler(PaymentValidationException.class)
    ResponseEntity<Object> handleBusinessValidation(PaymentValidationException exception, WebRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, exception.code(), exception.getMessage(), request);
    }

    @ExceptionHandler({ConstraintViolationException.class, IllegalArgumentException.class})
    ResponseEntity<Object> handleBadRequest(Exception exception, WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request validation failed.", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception exception, WebRequest request) {
        LOGGER.error("Unexpected request failure", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Use the correlation ID when reporting this issue.", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        var detail = buildProblem(status, "INVALID_REQUEST", "Request validation failed.", request);
        var violations = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .sorted((first, second) -> first.field().compareTo(second.field()))
                .toList();
        detail.setProperty("violations", violations);
        return new ResponseEntity<>(detail, headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        var code = status.is5xxServerError() ? "INTERNAL_ERROR" : "INVALID_REQUEST";
        var detail = buildProblem(status, code, "Request could not be processed.", request);
        return new ResponseEntity<>(detail, headers, status);
    }

    private ResponseEntity<Object> problem(HttpStatus status, String code, String detail, WebRequest request) {
        return ResponseEntity.status(status).body(buildProblem(status, code, detail, request));
    }

    private ProblemDetail buildProblem(HttpStatusCode status, String code, String detail, WebRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:flowpay:problem:" + code.toLowerCase(java.util.Locale.ROOT).replace('_', '-')));
        if (request instanceof ServletWebRequest servletRequest) {
            problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
        }
        problem.setProperty("code", code);
        problem.setProperty("correlationId", MDC.get("correlationId"));
        return problem;
    }

    private record FieldViolation(String field, String message) {
    }
}

