# Implementation plan: F6 explanatory source documentation

## 1. Specify the opt-in profile contract

- Add a typed `DocumentationSettings` accessor for `documentation.enabled`.
- Extend profile validation with a precise boolean-only violation.
- Lock default-disabled and invalid-type behavior with profile tests.

## 2. Build the deterministic documentation projection

- Add one shared formatter over `TargetModel` for canonical content and safe dynamic-text
  normalization.
- Cover ordering, empty collections, comment-terminator escaping, and repeatability with unit tests.

## 3. Apply target-specific comments

- Decorate Java artifacts in `JavaEmitter` at the package/import-to-declaration boundary.
- Decorate Node program artifacts in `DefaultNodeRenderer` while excluding project-shared files.
- Prove disabled byte identity and enabled traceability for both targets.

## 4. Verify and review

- Run focused profile/shared/Java/Node tests, followed by the full Maven reactor and clean install.
- Run `git diff --check`, inspect dependency and IR boundaries, and record verification and review
  reports.
- Advance Agora criteria through implemented and verified; leave acceptance human-gated.
