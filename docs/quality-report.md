# Verification record

Recorded on **2026-10-09** for the source delivery of FlowPay 1.0.0. These are results of executed local checks, not a claim that GitHub Actions, Docker or a production environment has passed.

## Environment and reproduction

| Item | Verified baseline |
|---|---|
| Runtime | OpenJDK 17.0.20, Linux x86-64 |
| Maven | 3.9.11, bootstrapped through the official Maven Wrapper |
| Framework | Spring Boot 4.1.1 |
| Coverage | JaCoCo 0.8.14 |
| Test command | `bash ./mvnw --batch-mode --no-transfer-progress clean verify` |
| Result | **BUILD SUCCESS** |

Dependencies were fetched before the final clean run; that run also succeeded with `--offline`. The download proxy used by the verification environment is not part of this repository and is not required by the application. A normal first run needs Maven Central access.

## Test results

**50 test invocations · 0 failures · 0 errors · 0 skipped**

Parameterized cases count individually. The fixture class is not a test suite.

| Suite | Cases | Behavior exercised |
|---|---:|---|
| `PaymentServiceTest` | 3 | Persistence, lookup, replay, events and missing receipts |
| `PaymentProcessorRegistryTest` | 3 | Routing for each method, duplicates and missing implementations |
| `PaymentRulesTest` | 17 | Amounts, precision, limits, currencies, tokens, rule ordering and fingerprint boundaries |
| `GatewayAdapterTest` | 2 | Exact conversion to minor units and provider-decline mapping |
| `InMemoryIdempotencyStoreTest` | 4 | Replay, payload conflict, failed reservations and concurrent deduplication |
| `PaymentApiTest` | 19 | Real HTTP requests against an embedded server on a random port |
| `ApiExceptionHandlerTest` | 2 | Sanitized server errors and correlation propagation |

The HTTP suite verifies creation, retrieval, replay headers, conflicting payloads, declines, malformed JSON, field/header validation, business-rule errors, correlation-ID sanitization, health and the payment counter. It uses the JDK HTTP client, not a mocked controller.

The concurrency test sends **24 requests through 12 worker threads** and asserts one provider-operation invocation and one non-replayed result. Latches coordinate the start and bounded waits prevent hanging tests. This is a local synchronization test, not a load benchmark or a distributed correctness proof.

## Measured coverage

The final clean run generated these aggregate JaCoCo counters:

| Counter | Covered / total | Ratio | Build gate |
|---|---:|---:|---:|
| Lines | 237 / 245 | **96.73%** | 90% |
| Branches | 51 / 56 | **91.07%** | 80% |
| Instructions | 1099 / 1131 | 97.17% | Informational |

The reported scope includes 33 instrumented classes. It excludes `FlowPayApplication`, `config/**` and `web/dto/**`; their wiring and serialization are exercised indirectly by the HTTP suite. Exclusions are declared in `pom.xml`, not applied after seeing the result. Ratios do not represent every line in the repository, and remaining error branches are not claimed as covered.

To inspect the generated reports after a build:

- Test results: `target/surefire-reports/`
- Coverage HTML: `target/site/jacoco/index.html`
- Coverage counters: `target/site/jacoco/jacoco.xml`

Generated reports, dependency caches and binaries are deliberately excluded from the source ZIP. Rebuild to reproduce them.

## Additional checks

| Check | Result and scope |
|---|---|
| Packaged JAR smoke test | Started the repackaged JAR; health was `UP`, creation returned `201`, replay returned `200`, retrieval returned `200`, and receipt bytes were unchanged. The test process was stopped afterward. |
| OpenAPI 3.1 schema | Validated with `openapi-spec-validator` 0.7.2. This is schema validation, not proof that every possible response conforms. |
| Domain boundary inspection | All 22 domain source files use only JDK/domain types; no Spring, Jakarta, HTTP or infrastructure imports. |
| Documentation links | Local Markdown links resolved to delivered files. |
| Banner | SVG rendered and visually inspected; it is a project banner, not a screenshot of a nonexistent UI. |
| Wrapper download | Maven distribution pinned to 3.9.11 with a SHA-256 checksum; bootstrap executed successfully. |
| CI references | Checkout and Java setup pinned to verified upstream v6 commit SHAs; workflow has read-only repository permissions. |
| Container image references | Both declared image tags existed in the official Docker Hub library during verification; images were not built or executed here. |

## Checks not performed

- Docker image build and Compose execution: no Docker engine was available.
- Windows wrapper execution or additional JDK versions: only Linux/JDK 17 was executed.
- Remote GitHub CI: the delivery has not been published by this verification process.
- Dependency vulnerability audit, penetration testing, production load or real provider integration.
- Restart recovery, multi-node idempotency or durable event delivery; these capabilities do not exist in this version.

A passing build and high measured coverage support this educational delivery. They are not security certification, production readiness or a guarantee that no defects remain.
