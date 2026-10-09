package dev.ricciarelli.flowpay.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentApiTest {

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Test
    void createsReplaysAndRetrievesAPayment() throws Exception {
        var key = UUID.randomUUID().toString();
        var body = payload("125.90", "BRL", "PIX", null);
        var first = post(body, key, "request_42");
        var replay = post(body, key, "request_43");
        String id = JsonPath.read(first.body(), "$.id");

        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(first.headers().firstValue("Location")).contains("/api/v1/payments/" + id);
        assertThat(first.headers().firstValue("Idempotent-Replayed")).contains("false");
        assertThat(first.headers().firstValue("X-Correlation-Id")).contains("request_42");
        assertThat(replay.statusCode()).isEqualTo(200);
        assertThat(replay.headers().firstValue("Idempotent-Replayed")).contains("true");
        assertThat(replay.body()).isEqualTo(first.body());

        var fetched = get("/api/v1/payments/" + id);
        assertThat(fetched.statusCode()).isEqualTo(200);
        assertThat(fetched.body()).isEqualTo(first.body());
        assertThat(first.body()).doesNotContain("paymentInstrumentToken", "description");
    }

    @Test
    void rejectsConflictingReuseOfAnIdempotencyKey() throws Exception {
        var key = UUID.randomUUID().toString();
        post(payload("10", "BRL", "PIX", null), key, null);
        var response = post(payload("11", "BRL", "PIX", null), key, "conflict_42");
        assertProblem(response, 409, "IDEMPOTENCY_CONFLICT");
        assertThat(JsonPath.<String>read(response.body(), "$.correlationId")).isEqualTo("conflict_42");
    }

    @Test
    void returnsADeclinedReceiptWithoutTreatingItAsAnHttpFailure() throws Exception {
        var response = post(payload("100", "USD", "CREDIT_CARD", "tok_declined"), UUID.randomUUID().toString(), null);
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(JsonPath.<String>read(response.body(), "$.status")).isEqualTo("DECLINED");
        assertThat(response.body()).doesNotContain("tok_declined");
    }

    @Test
    void processesBankTransfersAndNormalizesCurrencyCase() throws Exception {
        var response = post(payload("10.25", "eur", "BANK_TRANSFER", null), UUID.randomUUID().toString(), null);
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(JsonPath.<String>read(response.body(), "$.currency")).isEqualTo("EUR");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    void rejectsInvalidRequests(String name, String body, String key, int status, String code) throws Exception {
        assertProblem(post(body, key, null), status, code);
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("missing idempotency key", payload("10", "BRL", "PIX", null), null, 400, "INVALID_REQUEST"),
                Arguments.of("invalid key", payload("10", "BRL", "PIX", null), "bad", 400, "INVALID_REQUEST"),
                Arguments.of("zero amount", payload("0", "BRL", "PIX", null), "zero_amount", 400, "INVALID_REQUEST"),
                Arguments.of("excess precision", payload("1.001", "BRL", "PIX", null), "bad_precision", 400, "INVALID_REQUEST"),
                Arguments.of("amount limit", payload("100000.01", "BRL", "PIX", null), "over_limit", 422, "AMOUNT_LIMIT_EXCEEDED"),
                Arguments.of("invalid currency", payload("10", "ZZZ", "PIX", null), "bad_currency", 400, "INVALID_REQUEST"),
                Arguments.of("unsupported currency", payload("10", "JPY", "BANK_TRANSFER", null), "unsupported_currency", 422, "UNSUPPORTED_CURRENCY"),
                Arguments.of("pix in USD", payload("10", "USD", "PIX", null), "pix_usd_key", 422, "PIX_REQUIRES_BRL"),
                Arguments.of("missing card token", payload("10", "BRL", "CREDIT_CARD", null), "no_card_token", 422, "CARD_TOKEN_REQUIRED"),
                Arguments.of("invalid method", payload("10", "BRL", "CASH", null), "bad_method", 400, "INVALID_REQUEST"),
                Arguments.of("malformed JSON", "{broken", "broken_json", 400, "INVALID_REQUEST"),
                Arguments.of("missing fields", "{}", "missing_fields", 400, "INVALID_REQUEST"));
    }

    @Test
    void reportsMissingAndInvalidPaymentIds() throws Exception {
        assertProblem(get("/api/v1/payments/" + UUID.randomUUID()), 404, "PAYMENT_NOT_FOUND");
        assertProblem(get("/api/v1/payments/not-a-uuid"), 400, "INVALID_REQUEST");
    }

    @Test
    void replacesUnsafeCorrelationIds() throws Exception {
        var response = post(payload("10", "BRL", "PIX", null), UUID.randomUUID().toString(), "unsafe id <>");
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("X-Correlation-Id").orElseThrow())
                .matches("[0-9a-f-]{36}");
    }

    @Test
    void exposesHealthAndPaymentMetrics() throws Exception {
        post(payload("10", "BRL", "PIX", null), UUID.randomUUID().toString(), null);
        var health = get("/actuator/health");
        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(health.body(), "$.status")).isEqualTo("UP");
        var metric = get("/actuator/metrics/flowpay.payments.processed");
        assertThat(metric.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(metric.body(), "$.name")).isEqualTo("flowpay.payments.processed");
    }

    private HttpResponse<String> post(String body, String key, String correlationId) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/v1/payments"))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (key != null) request.header("Idempotency-Key", key);
        if (correlationId != null) request.header("X-Correlation-Id", correlationId);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private static String payload(String amount, String currency, String method, String token) {
        var tokenField = token == null ? "" : ",\"paymentInstrumentToken\":\"" + token + "\"";
        return "{\"customerId\":\"customer_42\",\"amount\":" + amount
                + ",\"currency\":\"" + currency + "\",\"method\":\"" + method + "\"" + tokenField + "}";
    }

    private static void assertProblem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow()).contains("application/problem+json");
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo(code);
        assertThat(JsonPath.<Integer>read(response.body(), "$.status")).isEqualTo(status);
        assertThat(response.body()).doesNotContain("stackTrace", "tok_");
    }
}
