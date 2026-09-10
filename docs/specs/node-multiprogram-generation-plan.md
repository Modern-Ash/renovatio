# Implementation plan: Node multi-program generation

## 1. Lock the regression with tests

- Add renderer tests proving canonical planned paths are emitted, shared artifacts are identical
  across programs, and generated TypeScript declarations are deterministic.
- Add provider aggregation coverage for equal shared artifacts and retain the existing conflicting
  duplicate-path test.
- Extend the CLI Node generation test to use two COBOL programs and assert the persisted project.

## 2. Make Node rendering project-composable

- Render every canonical TypeScript path carried by `TargetStructure` with a role-specific,
  standalone declaration.
- Remove program-specific data from `src/main.ts` and `package.json` so project files are stable.
- Normalize program identifiers separately for filesystem-safe slugs and TypeScript symbols.

## 3. Preserve collision safety

- Change project aggregation to accept only byte-identical duplicate artifacts.
- Continue rejecting differing content for the same path before the disk-write boundary.

## 4. Verify and review

- Run focused tests first, then the relevant Maven reactor and whitespace checks.
- Record verification and review artifacts, advance criteria through implemented and verified, and
  leave Spec Owner acceptance pending explicit human approval.

