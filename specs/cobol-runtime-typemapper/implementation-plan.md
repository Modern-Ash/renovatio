# Implementation Plan — COBOL runtime + rich PIC type model

**Agora work:** `delivery/cobol-runtime-typemapper`
**Method:** spec-driven · **Branch:** `agora/delivery`

## Context

COBOL→Java/Python transliteration in Renovatio currently loses numeric semantics.
`CobolTypeMapper.picToJavaType` returns only a target type *name* (`"Integer"`,
`"BigDecimal"`, …) and mis-classifies packed decimals (`COMP` → `Integer`). There is
no runtime library implementing COBOL data behaviour (fixed-point truncation,
`ROUNDED`, `ON SIZE ERROR`, `MOVE` padding/truncation, EBCDIC collation), so faithful
translation is impossible and every generated program would have to re-derive these
rules inline.

This work adds the semantic base: a zero-dependency `renovatio-cobol-runtime` module
plus a rich `PicType` descriptor that `renovatio-cobol-ir` consumes.

## Deliverables

### 1. New module `renovatio-cobol-runtime` (zero upward deps)

| Class | Responsibility | Status |
|---|---|---|
| `PicType` (record) | category, digits, scale, signed, usage (DISPLAY/COMP/COMP-3/COMP-5) | **done** |
| `PicClause` | parse a raw PICTURE (+USAGE) string → `PicType`; expands `9(n)`, handles `V`, `S`, `X`/`A`, COMP variants and separate signs | **done** |
| `CobolDecimal` | fixed-point value bound to a `PicType`; COBOL truncation, `ROUNDED`, and `ON SIZE ERROR` on store/add | **done** |
| `CobolMove` / `CobolAlphanumeric` | `MOVE` semantics: text padding/truncation and numeric receiving-picture truncation/size error | **done** |
| `EbcdicCollator` (`Comparator<String>`) | CP037 collating order for `IF`/`SORT` comparisons | **done** |

Module `pom.xml` registered in root `<modules>` before `renovatio-cobol-ir`.

### 2. Rework `CobolTypeMapper` (`renovatio-cobol-ir`)

- Add dependency on `renovatio-cobol-runtime`.
- New API `PicType picType(String pic)` delegating to `PicClause.parse`.
- Keep `picToJavaType(String)` signature; reimplement as `picType(pic)` → name map.
  **Existing outputs must not change** — `CobolTypeMapperTest` stays green.
- `COMP-3` / `V` → `BigDecimal`; `9(≤9)` → `Integer`; `9(10..18)` → `Long`; `>18` →
  `BigDecimal`; `X`/`A` → `String`. (Same table as today, now driven by `PicType`.)

### 3. Optional follow-up (separate work item, not here)

- `CobolDataItem` gains `PicType picType()` (additive; many call sites — own slice).
- Python mirror `cobol_runtime` package for `renovatio-provider-python`.

## TDD sequence (Iron Law: failing test first)

1. ✅ `PicClauseTest` — DISPLAY/COMP/COMP-3/COMP-5, scale, sign and `SIGN SEPARATE`.
2. ✅ `CobolDecimalTest` — truncation vs `ROUNDED` and `ON SIZE ERROR`, including
   receiving pictures with fractional scale.
3. ✅ `CobolMoveTest` / `CobolAlphanumericTest` — text pad/truncate and
   numeric-to-shorter-picture truncation.
4. ✅ `EbcdicCollatorTest` — digits > letters ordering; `a` vs `A`.
5. ✅ `CobolTypeMapperTest` — existing mappings remain green; rich `picType()` cases added.

Deferred edge cases (`P` scaling and edited pictures such as `Z`, currency symbols,
and explicit decimal punctuation) require an expanded `PicType` model and are not
claimed by this slice's acceptance criteria.

## Critical files

- `renovatio-cobol-runtime/**` (new)
- `renovatio-cobol-ir/pom.xml` — add runtime dep
- `renovatio-cobol-ir/src/main/java/org/shark/renovatio/cobol/ir/context/CobolTypeMapper.java`
- `renovatio-cobol-ir/src/main/java/org/shark/renovatio/cobol/ir/parser/SimpleCobolIrParser.java:214` — call site (unchanged behaviour)
- root `pom.xml` `<modules>`

## Verification

```bash
mvn -pl renovatio-cobol-runtime test
mvn -pl renovatio-cobol-ir test
mvn -pl renovatio-cobol-runtime,renovatio-cobol-ir test
```

Acceptance evidence per criterion: surefire reports for `PicClauseTest`,
`CobolDecimalTest`, `CobolAlphanumericTest`, and unchanged `CobolTypeMapperTest`.
