# Contributing

Keep changes small and tied to an observable behavior. This reference project does not accept real payment credentials, card data or customer records in code, fixtures, issues or logs.

Before a pull request:

1. Run `bash ./mvnw --batch-mode --no-transfer-progress clean verify` (Windows: `mvnw.cmd clean verify`).
2. Add a regression test for a changed behavior.
3. Update the OpenAPI contract and HTTP examples if the public contract changes.
4. Describe the decision and its limits; do not add a pattern solely to increase the pattern count.

The domain package should stay independent of Spring and HTTP/provider DTOs. Changes to concurrency, currencies, tokens or error handling need tests for failure and replay behavior, not only the happy path.
