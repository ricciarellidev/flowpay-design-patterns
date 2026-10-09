# Security policy

This repository is an educational reference implementation, not a production payment system.

Do not submit real card details, customer records or credentials. Payment instrument tokens in the examples are fictional fixtures. The service has no authentication, persistent storage or tenant isolation and should not be exposed on a public network.

## Reporting a concern

Use the repository's private vulnerability reporting feature if it is enabled. Otherwise, open a minimal issue requesting a private reporting channel without including exploit details, tokens or customer data. Do not publish sensitive information in an issue or pull request.

## Maintenance

Only the latest main-branch version is maintained. Dependency changes should pass the full `verify` build and be reviewed before merge. A green build is evidence of the automated checks, not a security certification.
