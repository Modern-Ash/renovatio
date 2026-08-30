# Implementation Plan: Deterministic COBOL Semantic Core

> GitHub issue: [#123](https://github.com/Modern-Ash/renovatio/issues/123)  
> Agora work: `ai-modernization/deterministic-semantic-core`  
> Specification: `docs/specs/deterministic-semantic-core.md`

## 1. Outcome

Replace raw-string interpretation for the supported COBOL subset with typed deterministic IR,
PIC-aware runtime operations, typed level-88 predicates, and pure AST-safe OpenRewrite rendering.
Every supported construct receives focused and characterization coverage; recognized unsupported
forms fail closed with the #122 manual-action contract.

## 2. Delivery sequence

### Step 0 — Satisfy the #122 dependency

- Bring commit `bbd35be` into this branch, either directly or through the merged `main` history.
- Confirm the strict schemas, action-item enums/redaction, ordered gate runner, and fixture contract
  are present.
- Do not begin source implementation until `git merge-base --is-ancestor bbd35be HEAD` succeeds.

### Step 1 — Introduce typed expressions, conditions, and diagnostics

- Add sealed or closed IR node families for numeric/string literals, data references, unary and
  binary arithmetic, comparisons, boolean operators, and level-88 references.
- Add source spans and stable node identity to executable nodes needed by action items.
- Add structured diagnostics for recognized unsupported forms; no recipe may emit executable Java
  from an invalid or opaque expression.
- Preserve compatibility through explicit adapters only where current tests require it, then remove
  raw-string interpretation from the recipe path.

### Step 2 — Make data items PIC-aware and model level-88

- Carry the parsed runtime `PicType` descriptor on every executable elementary data item.
- Treat malformed PIC clauses as diagnostics rather than silently executable `String` fields.
- Model level-88 entries under their immediately preceding condition variable with single values,
  multiple values, and inclusive ranges.
- Implement the approved typed predicate/value representation with named condition methods and no
  allocated level-88 storage.
- Add deterministic Java-name collision detection.

### Step 3 — Parse the supported statement subset deterministically

- Replace statement-level splitting where necessary with a bounded tokenizer and recursive-descent
  parser for the specified subset.
- Parse one-to-many `MOVE`, figurative constants, and typed receivers.
- Parse `COMPUTE` precedence, parentheses, unary signs, and the four approved arithmetic operators.
- Parse nested `IF` with explicit `END-IF`, supported comparisons, and boolean composition.
- Parse single-subject `EVALUATE`, level-88/literal alternatives, and `WHEN OTHER` in source order.
- Parse simple out-of-line `PERFORM`, while rejecting recursion, missing targets, `THRU`, inline,
  `TIMES`, `UNTIL`, and `VARYING` through structured diagnostics.

### Step 4 — Implement PIC-aware runtime operations

- Add receiving-field operations for alphanumeric padding/truncation and the approved figurative
  constants.
- Use `BigDecimal` for decimal arithmetic and apply `RoundingMode.DOWN` when scale is reduced without
  `ROUNDED`.
- Enforce target sign, scale, digit capacity, and integral Java range.
- Fail closed on overflow or unsupported category conversion; do not silently wrap, round, or use
  floating point.

### Step 5 — Render pure deterministic Java through OpenRewrite

- Update `PopulateCobolProcessRecipe` to render validated typed nodes only.
- Generate structured `if`, `switch`, assignments, arithmetic, predicate calls, and acyclic
  paragraph invocation through AST-safe templates.
- Replace current comment-only substitutes for unsupported executable forms with deterministic
  refusal and action items at the orchestration boundary.
- Add an architectural dependency test proving the recipe modules contain no provider SDK, HTTP
  client, prompt, credential, or network path.
- Run generation twice and compare byte hashes after the repository formatter.

### Step 6 — Build the construct-to-test matrix

- Complete the exact matrix in specification section 9.1 using the #122 fixture conventions.
- Add the supported fixtures `move-numeric`, `move-alphanumeric-boundaries`,
  `compute-decimal-sign`, `if-nested`, `evaluate-level-88`, and `perform-simple-nested`.
- Add focused boundary tests for PIC mappings, level-88 values/ranges/overlap, arithmetic overflow,
  recursion, missing targets, unsupported forms, stable identifiers, and byte reproducibility.
- Ensure rejection fixtures contain no speculative Java and contain a schema-valid action item.

### Step 7 — Verify and produce governed evidence

- Run unit tests for runtime, IR, recipes, and provider modules on Java 17.
- Run the #122 ordered gate runner and affected characterization selection.
- Execute the deterministic subset twice and record identical SHA-256 hashes.
- Register the test plan before final execution and the successful test report afterward.
- Keep #123 out of `verified` until the required #122 characterization and offline checks are green.

## 3. Module ownership

| Module | Planned responsibility |
| --- | --- |
| `renovatio-cobol-runtime` | PIC receiving semantics, exact decimal arithmetic, range enforcement |
| `renovatio-cobol-ir` | Typed expressions/conditions, PIC-aware data items, level-88, parser diagnostics |
| `cobol-openrewrite-recipes` | Pure AST-safe rendering from validated typed IR |
| `renovatio-provider-cobol` | Orchestration, fail-closed action items, characterization harness |

## 4. Acceptance coverage

| Criterion | Planned coverage |
| --- | --- |
| `statements` | Steps 1, 3, 5, and 6 cover the five statement families from parser through Java behavior. |
| `data-model` | Steps 2, 4, and 6 cover rich PIC semantics and typed level-88 predicates. |
| `pure-recipes` | Step 5 enforces the provider-free boundary and byte reproducibility. |
| `characterized` | Steps 6 and 7 implement and evidence the explicit construct-to-test matrix. |

## 5. Verification commands

```bash
mvn -B -pl renovatio-cobol-runtime,renovatio-cobol-ir,cobol-openrewrite-recipes,renovatio-provider-cobol -am test
mvn -B -pl renovatio-provider-cobol,cobol-openrewrite-recipes -am -DskipTests package
```

The final verification also runs the affected characterization selection twice and compares
canonical IR and Java SHA-256 hashes. All commands use Java 17; offline verification remains governed
by the #122 lane.

## 6. Risks and controls

- **Ambiguous COBOL scope:** v1 nested IF requires `END-IF`; ambiguous period scope fails closed.
- **Numeric semantic drift:** all decimal operations use `BigDecimal`, target PIC constraints, and
  the approved truncation policy.
- **Level-88 misrepresentation:** named predicates preserve multiple values, ranges, and overlap;
  no enum ordinal semantics are introduced.
- **Parser scope creep:** every excluded phrase has a rejection test and action item instead of an
  opportunistic partial translation.
- **Recipe impurity:** dependency tests and two-run hashes enforce the deterministic boundary.
- **Incomplete dependency:** Step 0 gates implementation and Step 7 separately gates verification.

## 7. Planning approval gate

Before transition to `planned`, `project:owner` must approve this sequence and mark all four
criteria at stage `planned`. Transition to `implementing` additionally requires Step 0 to be true.
