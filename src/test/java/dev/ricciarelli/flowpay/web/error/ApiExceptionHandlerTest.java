package dev.ricciarelli.flowpay.web.error;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();
    private final ServletWebRequest request = new ServletWebRequest(
            new MockHttpServletRequest("POST", "/api/v1/payments"));

    @Test
    void sanitizesUnexpectedFailuresAndPreservesTheTraceId() {
        MDC.put("correlationId", "error_test_001");
        try {
            var response = handler.handleUnexpected(new IllegalStateException("Internal diagnostic only"), request);
            var problem = (ProblemDetail) response.getBody();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(problem).isNotNull();
            assertThat(problem.getDetail()).doesNotContain("Internal diagnostic only");
            assertThat(problem.getInstance()).isEqualTo(URI.create("/api/v1/payments"));
            assertThat(problem.getProperties()).containsEntry("code", "INTERNAL_ERROR")
                    .containsEntry("correlationId", "error_test_001");
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Test
    void classifiesFrameworkServerErrorsWithoutExposingTheirBody() {
        var response = handler.handleExceptionInternal(new IllegalStateException("Internal diagnostic only"),
                "Sensitive internal body", HttpHeaders.EMPTY, HttpStatus.INTERNAL_SERVER_ERROR, request);
        var problem = (ProblemDetail) response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(problem).isNotNull();
        assertThat(problem.getDetail()).doesNotContain("Sensitive", "diagnostic");
        assertThat(problem.getProperties()).containsEntry("code", "INTERNAL_ERROR");
    }
}
