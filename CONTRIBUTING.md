# Contributing to Renovatio

Thanks for helping improve Renovatio. This project uses governed, evidence-based
changes: issues describe acceptance criteria, pull requests include verification,
and architecture work is tracked through Agora records.

## Before You Start

- Open or reference a GitHub issue for non-trivial changes.
- Keep pull requests focused on one issue or review finding.
- Do not commit generated build output, local databases, secrets, personal data,
  dependency caches, or machine-local IDE state.
- Run the narrowest meaningful tests for your change and list them in the PR.

## Developer Certificate of Origin

Renovatio uses the Developer Certificate of Origin 1.1. By contributing, you
certify that you have the right to submit the work under the repository license.

Sign off each commit:

```bash
git commit -s -m "type(scope): summary"
```

The sign-off line should look like:

```text
Signed-off-by: Your Name <you@example.com>
```

## Pull Request Checklist

- The PR references its issue.
- Public behavior and documentation are updated together.
- Tests or scan results are attached when relevant.
- New generated files are intentionally reviewed and allowed by `.gitignore`.
- Security-sensitive changes include a short risk note.

## License

This repository is licensed under the MIT License. Do not copy external code
into this repository unless its license is documented and compatible with MIT
and the repository's dependency obligations.
