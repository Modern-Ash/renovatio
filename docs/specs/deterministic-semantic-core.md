# Deterministic COBOL Semantic Core

> GitHub issue: [#123](https://github.com/Modern-Ash/renovatio/issues/123)  
> Agora work: `ai-modernization/deterministic-semantic-core`  
> Lifecycle stage: drafting

## 1. Purpose

Define the deterministic COBOL subset owned by the parser, intermediate representation, runtime,
and pure OpenRewrite recipes. Identical source and configuration must produce byte-identical IR and
Java without prompts, provider clients, credentials, cache lookups, or network access.

This work builds on the guardrail contracts from issue #122. It does not implement `GO TO`, complex
`PERFORM`, `REDEFINES`, `OCCURS DEPENDING ON`, domain naming, or any LLM-assisted interpretation.

## 2. Supported statement subset

### 2.1 MOVE

- One source and one or more targets.
- Numeric literals and elementary numeric data items.
- Quoted alphanumeric literals and elementary alphanumeric data items.
- Figurative constants `ZERO`, `ZEROS`, `ZEROES`, `SPACE`, and `SPACES`.
- Assignment uses the receiving target's PIC metadata for scale, sign, padding, and truncation.
- Unsupported qualification, reference modification, CORRESPONDING, or incompatible category
  conversion fails closed with a diagnostic and manual action item.

### 2.2 COMPUTE

- One target and an arithmetic expression containing numeric literals, data items, parentheses,
  unary sign, and `+`, `-`, `*`, `/`.
- Decimal arithmetic uses `BigDecimal`; floating-point arithmetic is forbidden.
- The receiving target's PIC metadata determines final scale and range handling.
- `ROUNDED`, `ON SIZE ERROR`, multiple receiving targets, exponentiation, and intrinsic functions
  are outside this slice and fail closed.

### 2.3 IF

- Nested `IF`/`ELSE` with explicit `END-IF` scope. Period-terminated nested scope is outside v1.
- Comparisons `=`, `NOT =`, `<>`, `<`, `<=`, `>`, and `>=`.
- Boolean composition with `AND`, `OR`, and `NOT`, preserving COBOL precedence explicitly in IR.
- Conditions may reference a supported level-88 condition name.
- Abbreviated combined relations and dialect-specific condition forms are outside this slice.

### 2.4 EVALUATE

- One subject with literal or level-88 `WHEN` alternatives and optional `WHEN OTHER`.
- Branch order is preserved in IR and observable behavior.
- `EVALUATE TRUE`, multiple subjects, `ALSO`, ranges, partial expressions, and `THRU` alternatives
  are outside this slice.

### 2.5 PERFORM

- Out-of-line `PERFORM <paragraph>` for an existing paragraph.
- Nested acyclic paragraph calls are supported.
- Missing targets and recursion fail closed instead of emitting comments as executable substitutes.
- `THRU`, inline bodies, `TIMES`, `UNTIL`, `VARYING`, and recursive execution are outside this slice.

## 3. Typed data model

Every elementary data item used by the supported statements carries a parsed `PicType`, not only a
legacy Java type string. The deterministic model records category, digit count, scale, sign,
storage usage, declared size, and source identity. Unsupported or malformed PIC clauses produce a
diagnostic; they do not silently default to `String` for an executable translation.

The minimum basic mapping is:

| COBOL category | Java representation |
| --- | --- |
| Alphabetic or alphanumeric | `String` with target-width semantics |
| Integral numeric, up to 9 digits | `int` when the declared range fits |
| Integral numeric, 10–18 digits | `long` when the declared range fits |
| Scaled or larger numeric | `BigDecimal` |

Level-88 entries are attached to their immediately preceding condition variable. Each entry retains
one or more literal values and inclusive ranges. Generated Java exposes a typed predicate/value
object with named condition methods whose evaluation delegates to the condition variable's
PIC-aware value. An enum is explicitly not used because conditions may contain multiple values,
ranges, or overlap. No ordinal is invented and no level-88 entry allocates storage.

## 4. Structured IR

Supported executable semantics must not remain opaque strings. The v1 IR adds typed expression and
condition nodes for literals, data references, unary/binary arithmetic, comparisons, boolean
operators, and level-88 references. Statement nodes refer to typed data identities and source spans.

The parser preserves source order and returns diagnostics for recognized-but-unsupported syntax.
The translator consumes only validated typed nodes. It must never reinterpret raw COBOL text in an
OpenRewrite recipe.

## 5. Runtime semantics

- Alphanumeric receiving fields apply COBOL width rules deterministically: shorter values are
  space-padded and longer values are truncated at the receiving boundary.
- Numeric receiving fields apply the declared sign, digits, and scale without binary floating point.
- Division and scale reduction without `ROUNDED` truncate toward zero using
  `java.math.RoundingMode.DOWN`; the policy cannot depend on host locale.
- A value outside the representable target range fails closed unless a later specification adds the
  relevant COBOL error-handling phrase.
- Java identifiers are derived through one locale-independent naming function with collision
  detection.

## 6. Pure recipe boundary

`CobolSemanticTranspiler` injects the validated base IR through `ExecutionContext`.
`PopulateCobolProcessRecipe` and any supporting recipes perform AST-safe edits from that model only.
The recipe classpath and execution path contain no HTTP client, provider SDK, prompt catalog,
credential lookup, or model-dependent branch.

Generated Java is formatted once with repository configuration. Timestamps, random identifiers,
absolute paths, environment-specific line endings, and unordered collection iteration are forbidden
in generated sources.

## 7. Characterization requirements

The implementation consumes the fixture conventions established by issue #122 and must cover at
least:

- numeric and alphanumeric MOVE boundaries;
- signed and scaled COMPUTE results;
- true, false, nested, and composed IF conditions;
- EVALUATE literal, level-88, and OTHER branches;
- simple and nested acyclic PERFORM;
- PIC category and Java representation boundaries;
- level-88 single values, multiple values, and ranges;
- rejection of every explicitly unsupported form above.

For each supported fixture, tests compare canonical IR, generated Java, observable outputs, state
changes, and byte-stable hashes across two runs. Rejection fixtures prove there is no speculative
Java and that a schema-valid manual action item identifies the unsupported construct.

## 8. Dependency gate

Implementation may begin once the issue #122 contracts used here are committed and available on the
branch: the versioned schemas, action-item report, ordered gate runner, and agreed fixture
directory/file conventions. The complete offline CI lane may be delivered concurrently, but #123
cannot be marked verified until the #122 characterization and offline gates it relies on are green.
The branch must contain commit `bbd35be` (`fix(cobol): harden guardrail contracts`) or a merge commit
whose history includes it.

## 9. Acceptance mapping

| Agora criterion | Specification obligation |
| --- | --- |
| `statements` | Section 2 defines the supported deterministic statement subset and exclusions. |
| `data-model` | Sections 3–5 define PIC-aware types, level-88 semantics, and receiving-field behavior. |
| `pure-recipes` | Section 6 defines the provider-free, byte-stable recipe boundary. |
| `characterized` | Sections 7–8 define fixture coverage, rejection behavior, reproducibility, and dependency gates. |

### 9.1 Construct-to-test matrix

| Construct | Required fixtures or focused tests |
| --- | --- |
| Numeric `MOVE` | `move-numeric`; target range, sign, and numeric receiver tests |
| Alphanumeric `MOVE` | `move-alphanumeric-boundaries`; padding, truncation, and figurative constants |
| `COMPUTE` | `compute-decimal-sign`; precedence, unary sign, division, scale truncation, and overflow rejection |
| Nested and composed `IF` | `if-nested`; true/false branches, boolean precedence, and missing `END-IF` rejection |
| `EVALUATE` | `evaluate-level-88`; literal, level-88, ordered branch, and `WHEN OTHER` coverage |
| Simple `PERFORM` | `perform-simple-nested`; acyclic nesting, missing target, recursion, and unsupported form rejection |
| Basic PIC mapping | Boundary tests for `String`, `int`, `long`, and `BigDecimal`, including malformed PIC rejection |
| Level-88 | Single value, multiple values, inclusive range, overlap, named predicates, and no-storage behavior |
| Pure recipes | Two-run byte hash comparison plus a dependency scan proving no provider or network path |

## 10. Resolved clarification decisions

On 2026-08-30, `project:owner` accepted these decisions for v1:

1. `COMPUTE` without `ROUNDED` truncates toward zero with `RoundingMode.DOWN`.
2. Level-88 generates a typed predicate/value object rather than an enum.
3. Nested `IF` requires explicit `END-IF`; period-terminated nested scope is outside v1.
4. Implementation may start from committed #122 contracts, while verification remains gated on the
   required #122 characterization and offline checks becoming green.
5. The canonical `spec` artifact is `repo://docs/specs/deterministic-semantic-core.md`; repeated
   registrations of that URI are revisions of the same artifact.
6. The implementation dependency requires commit `bbd35be` or a merge history containing it.
7. Section 9.1 is the required construct-to-test coverage matrix.
8. `project:owner`, acting as Spec Owner, marks all clarification questions resolved and approves
   this specification for the `spec-clarified` gate.
