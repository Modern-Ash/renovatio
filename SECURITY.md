# Security Policy

## Reporting a Vulnerability

Please do not open public issues for suspected vulnerabilities.

Report security concerns privately to the repository owner or through GitHub
private vulnerability reporting when enabled. Include:

- Affected component and version or commit.
- Reproduction steps or proof of concept.
- Potential impact.
- Any known mitigations.

We will acknowledge reports as soon as possible, triage severity, and coordinate
fix disclosure through the relevant issue or release notes after mitigation.

## Supported Versions

Renovatio is currently pre-1.0. Security fixes target the default branch and the
latest published technical preview.

## Secret Handling

Never commit credentials, API tokens, private keys, local databases, customer
data, or personally identifiable information. Use environment variables,
repository secrets, or local ignored files for sensitive configuration.
