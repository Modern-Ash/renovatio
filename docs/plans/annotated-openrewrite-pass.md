# Annotated OpenRewrite Pass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the deterministic OpenRewrite translation pass consume a validated `AnnotatedCobolModel` sidecar, applying only `ACCEPTED` `DOMAIN_NAMING` and `DATA_INTENT` annotations through AST-safe transforms, with a deterministic fallback plus manual action items for every other case.

**Architecture:** A new pure module `renovatio-cobol-annotations` publishes the `@CobolDataIntent` marker. A new `AnnotationApplicator` in `cobol-openrewrite-recipes` runs as a post-processing visitor inside `PopulateCobolProcessRecipe`, reads `AnnotatedCobolContext` from the existing `ExecutionContext` seam, filters annotations by eligibility, applies renames and markers via OpenRewrite AST operations, and records dropped annotations as neutral outcome records in the `ExecutionContext`. In `renovatio-provider-cobol`, a new `AnnotatedContextResolver` sources the sidecar (request > committed path > legacy) and `CobolSemanticTranspiler` drains the outcome records and maps them to `ManualActionItem`s via `ManualActionItemWriter`.

**Tech Stack:** Java 17, Maven multi-module, OpenRewrite 8.21.0 (`rewrite-java`), JUnit 5, AssertJ, Jackson, `com.networknt` JSON Schema validator.

**Spec:** `docs/specs/annotated-openrewrite-pass.md`

## Global Constraints

- `cobol-openrewrite-recipes`, `renovatio-cobol-ir`, and `renovatio-cobol-annotations` must not depend on `org.shark.renovatio.provider.*`, any HTTP client, any credential resolver, or any prompt catalog. The recipe-boundary architecture test and Maven Enforcer rule enforce this.
- All AST changes use OpenRewrite operations (`JavaTemplate`, `RenameVariable`, `ChangeFieldName`, `ChangeMethodName`, visitor rewrites). Raw string replacement on source text is forbidden.
- Determinism: given the same base `CobolIntermediateModel` and the same validated `AnnotatedCobolModel`, the generated Java is byte-identical across runs. Annotation processing order is `(nodeId, annotationId)`. Never read wall clock, environment, random, or map iteration order.
- Sidecar schema version is exactly `cobol-annotated-ir.v1`. Unknown or mismatched versions fail closed to deterministic translation plus a manual action item. No best-effort conversion.
- Eligibility for application: `sidecar.baseIrHash == CobolIrIdentityProjector.baseIrHash(model)`, sidecar passes `AnnotatedCobolValidator` with zero diagnostics, `annotation.review.reviewState == ACCEPTED`, and `annotation.nodeId` resolves to exactly one base node whose kind matches `annotation.nodeKind`.
- Maven group/version for new module: `org.shark.renovatio` / `0.0.1-SNAPSHOT`, parent `renovatio-parent`.
- `manual-action-item.v1` field enums: `failedGate` ∈ {`schema`,`compilation`,`characterization`,`review-eligibility`}; `severity` ∈ {`warning`,`error`,`critical`}; `reviewStatus` ∈ {`pending`,`accepted`,`rejected`,`resolved`}; `id` matches `^mai-[a-f0-9]{24}$` (use `ManualActionItemIds.from(...)`).

---

## File Structure

**New — `renovatio-cobol-annotations/`**
- `pom.xml` — zero-dependency module (test-only JUnit).
- `src/main/java/org/shark/renovatio/cobol/annotations/CobolDataIntent.java` — the `@interface`.
- `src/test/java/org/shark/renovatio/cobol/annotations/CobolDataIntentTest.java` — retention/target reflection test.

**New — `cobol-openrewrite-recipes/src/main/java/org/shark/renovatio/cobol/recipes/annotate/`**
- `AnnotationApplicator.java` — filters eligible annotations, applies them to a `J.CompilationUnit`, returns `AnnotationApplicationOutcome`.
- `AnnotationApplicationOutcome.java` — record: `J.CompilationUnit tree`, `List<DroppedAnnotation> dropped`.
- `DroppedAnnotation.java` — record: `String nodeId`, `String annotationId`, `AnnotationFamily family`, `DropReason reason`, `String detail`. Enum `DropReason { REJECTED, PENDING_REVIEW, STALE_SIDECAR, NAME_COLLISION, NODE_UNRESOLVED, FAMILY_NOT_APPLIED }`.
- `NodeIdentityIndex.java` — wraps `CobolIrIdentityProjector.nodes(model)` as `Map<String, ProjectedNode>` keyed by `nodeId`, plus pointer→Java-identifier derivation.
- `AnnotationApplicationKey.java` — holds the `ExecutionContext` message key constant `renovatio.cobol.annotation-outcomes`.

**Modified — `cobol-openrewrite-recipes`**
- `PopulateCobolProcessRecipe.java` — after body render, run `AnnotationApplicator`; push `List<DroppedAnnotation>` into `ExecutionContext`.
- `pom.xml` — add `renovatio-cobol-annotations` (compile).

**New — `renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/translation/`**
- `AnnotatedContextResolver.java` — resolves `Optional<AnnotatedCobolContext>` from request sidecar / committed path / legacy.
- `AnnotationActionItemFactory.java` — maps `DroppedAnnotation` → `ManualActionItem`.

**Modified — `renovatio-provider-cobol`**
- `CobolSemanticTranspiler.java` — new `enrichServiceImplementation(String, AnnotatedCobolContext, ActionItemSink)` path; drain outcome key; hand items to sink.
- `service/JavaGenerationService.java:87` — resolve annotated context, pass through, write action items via `ManualActionItemWriter`.
- `CharacterizationFixtureContractTest.java` — annotated fixture branch.
- `pom.xml` — add `renovatio-cobol-annotations` (test, for fixture compilation) if needed.

**Modified — root**
- `pom.xml` — add `<module>renovatio-cobol-annotations</module>`.

**New — test resources**
- `renovatio-provider-cobol/src/test/resources/characterization/move-numeric/move-numeric.annotated.json` + `expected-annotated.java`.
- A new `data-intent-redefines` fixture directory (input.cob, expected-ir.json, expected-behavior.json, expected-action-items.json, translation-input.java, `data-intent-redefines.annotated.json`, `expected-annotated.java`).

---

## Task 1: `renovatio-cobol-annotations` module and `@CobolDataIntent`

**Files:**
- Create: `renovatio-cobol-annotations/pom.xml`
- Create: `renovatio-cobol-annotations/src/main/java/org/shark/renovatio/cobol/annotations/CobolDataIntent.java`
- Create: `renovatio-cobol-annotations/src/test/java/org/shark/renovatio/cobol/annotations/CobolDataIntentTest.java`
- Modify: `pom.xml` (root) — add module entry after `<module>cobol-openrewrite-recipes</module>`

**Interfaces:**
- Consumes: nothing.
- Produces: `@CobolDataIntent` with `String nodeId()`, `String annotationId()`, `Construction construction()`, `String interpretation()`, `String[] assumptions()`; nested `enum Construction { REDEFINES, OCCURS_DEPENDING_ON }`; `RUNTIME` retention; `{FIELD, TYPE}` targets.

- [ ] **Step 1: Create the module pom**

`renovatio-cobol-annotations/pom.xml` (model on `renovatio-cobol-runtime/pom.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.shark.renovatio</groupId>
        <artifactId>renovatio-parent</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>
    <artifactId>renovatio-cobol-annotations</artifactId>
    <name>Renovatio COBOL Annotations</name>
    <description>Zero-dependency marker annotations attached to generated Java by the deterministic OpenRewrite pass. Carries reviewed COBOL data-layout intent for traceability; changes no semantics.</description>
    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <useModulePath>false</useModulePath>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Register the module in the root pom**

In `pom.xml`, add inside `<modules>` immediately after `<module>cobol-openrewrite-recipes</module>`:

```xml
        <module>renovatio-cobol-annotations</module>
```

- [ ] **Step 3: Write the failing test**

`CobolDataIntentTest.java`:

```java
package org.shark.renovatio.cobol.annotations;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobolDataIntentTest {

    @CobolDataIntent(nodeId = "n", annotationId = "a",
            construction = CobolDataIntent.Construction.REDEFINES,
            interpretation = "overlay", assumptions = {"x"})
    private String annotated;

    @Test
    void retainsAtRuntimeWithFieldAndTypeTargets() {
        Retention retention = CobolDataIntent.class.getAnnotation(Retention.class);
        Target target = CobolDataIntent.class.getAnnotation(Target.class);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
        assertTrue(List.of(target.value()).containsAll(List.of(ElementType.FIELD, ElementType.TYPE)));
    }

    @Test
    void exposesPayloadThroughReflection() throws NoSuchFieldException {
        CobolDataIntent a = CobolDataIntentTest.class.getDeclaredField("annotated")
                .getAnnotation(CobolDataIntent.class);
        assertEquals("n", a.nodeId());
        assertEquals(CobolDataIntent.Construction.REDEFINES, a.construction());
        assertEquals(List.of("x"), List.of(a.assumptions()));
    }
}
```

- [ ] **Step 4: Run test to verify it fails**

Run: `mvn -q -pl renovatio-cobol-annotations test -o`
Expected: FAIL — `CobolDataIntent` does not compile (type missing).

- [ ] **Step 5: Write the annotation**

`CobolDataIntent.java`:

```java
package org.shark.renovatio.cobol.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a generated Java field or type with reviewed COBOL data-layout intent.
 * Informational only: it changes no field type, initializer, accessor, or control flow.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface CobolDataIntent {

    String nodeId();

    String annotationId();

    Construction construction();

    String interpretation();

    String[] assumptions();

    enum Construction { REDEFINES, OCCURS_DEPENDING_ON }
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `mvn -q -pl renovatio-cobol-annotations test -o`
Expected: PASS (2 tests).

- [ ] **Step 7: Verify the reactor still resolves**

Run: `mvn -q -pl renovatio-cobol-annotations -am install -DskipTests -Djacoco.skip=true -o`
Expected: BUILD SUCCESS.

- [ ] **Step 8: Commit**

```bash
git add pom.xml renovatio-cobol-annotations
git commit -m "feat(cobol): add renovatio-cobol-annotations module with @CobolDataIntent"
```

---

## Task 2: `NodeIdentityIndex` — resolve annotation nodeId to a Java identifier

**Files:**
- Create: `cobol-openrewrite-recipes/src/main/java/org/shark/renovatio/cobol/recipes/annotate/NodeIdentityIndex.java`
- Test: `cobol-openrewrite-recipes/src/test/java/org/shark/renovatio/cobol/recipes/annotate/NodeIdentityIndexTest.java`

**Interfaces:**
- Consumes: `CobolIntermediateModel`, `CobolIrIdentityProjector` (`nodes(model)` → `List<ProjectedNode>` with `nodeId`, `nodeKind`, `pointer`), `AnnotatedNodeKind`.
- Produces:
  - `NodeIdentityIndex(CobolIntermediateModel model)` constructor.
  - `Optional<Resolved> resolve(String nodeId, AnnotatedNodeKind expectedKind)` — empty when the id is absent or the kind disagrees.
  - `record Resolved(String cobolName, AnnotatedNodeKind kind, String pointer)`.
  - `static String toJavaFieldName(String cobolName)` / `toJavaAccessorStem(String cobolName)` / `toJavaMethodName(String cobolName)` — same casing rules the recipe already uses (`toPascal` for stems/methods-as-pascal-then-decap, camel for fields).

- [ ] **Step 1: Write the failing test**

```java
package org.shark.renovatio.cobol.recipes.annotate;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind;
import org.shark.renovatio.cobol.ir.annotated.CobolIrIdentityProjector;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.cobol.ir.parser.SimpleCobolIrParser;

import static org.assertj.core.api.Assertions.assertThat;

class NodeIdentityIndexTest {

    private static final String COBOL = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. SAMPLE.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 CUSTOMER-NAME PIC X(30).
            PROCEDURE DIVISION.
            MAIN-PARA.
                MOVE 'A' TO CUSTOMER-NAME.
            """;

    private CobolIntermediateModel model() {
        return new SimpleCobolIrParser().parse(COBOL); // use the project's real parser entrypoint
    }

    @Test
    void resolvesDataItemNodeIdToCobolName() {
        CobolIntermediateModel model = model();
        CobolIrIdentityProjector projector = new CobolIrIdentityProjector();
        String dataItemNodeId = projector.nodes(model).stream()
                .filter(n -> n.nodeKind() == AnnotatedNodeKind.DATA_ITEM)
                .findFirst().orElseThrow().nodeId();

        NodeIdentityIndex index = new NodeIdentityIndex(model);
        var resolved = index.resolve(dataItemNodeId, AnnotatedNodeKind.DATA_ITEM);

        assertThat(resolved).isPresent();
        assertThat(resolved.get().cobolName()).isEqualTo("CUSTOMER-NAME");
    }

    @Test
    void returnsEmptyOnKindMismatchOrUnknownId() {
        NodeIdentityIndex index = new NodeIdentityIndex(model());
        assertThat(index.resolve("deadbeef".repeat(8), AnnotatedNodeKind.DATA_ITEM)).isEmpty();
    }

    @Test
    void derivesJavaIdentifiers() {
        assertThat(NodeIdentityIndex.toJavaFieldName("CUSTOMER-NAME")).isEqualTo("customerName");
        assertThat(NodeIdentityIndex.toJavaAccessorStem("CUSTOMER-NAME")).isEqualTo("CustomerName");
    }
}
```

> Note: confirm the exact parser entrypoint (`SimpleCobolIrParser` vs `CobolIntermediateModelService`). `CobolIntermediateModelService` lives in `renovatio-provider-cobol`; from the recipes module use `renovatio-cobol-ir`'s parser (`org.shark.renovatio.cobol.ir.parser.SimpleCobolIrParser`). Adjust the import if the class name differs.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl cobol-openrewrite-recipes test -o -Dtest=NodeIdentityIndexTest`
Expected: FAIL — `NodeIdentityIndex` missing.

- [ ] **Step 3: Implement `NodeIdentityIndex`**

```java
package org.shark.renovatio.cobol.recipes.annotate;

import org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind;
import org.shark.renovatio.cobol.ir.annotated.CobolIrIdentityProjector;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Resolves an annotation {@code nodeId} to the COBOL name of the base node it addresses. */
public final class NodeIdentityIndex {

    public record Resolved(String cobolName, AnnotatedNodeKind kind, String pointer) {}

    private final Map<String, CobolIrIdentityProjector.ProjectedNode> byId = new LinkedHashMap<>();

    public NodeIdentityIndex(CobolIntermediateModel model) {
        for (CobolIrIdentityProjector.ProjectedNode node : new CobolIrIdentityProjector().nodes(model)) {
            byId.putIfAbsent(node.nodeId(), node);
        }
    }

    public Optional<Resolved> resolve(String nodeId, AnnotatedNodeKind expectedKind) {
        CobolIrIdentityProjector.ProjectedNode node = byId.get(nodeId);
        if (node == null || node.nodeKind() != expectedKind) {
            return Optional.empty();
        }
        return Optional.of(new Resolved(lastPointerSegment(node.pointer()), node.nodeKind(), node.pointer()));
    }

    private static String lastPointerSegment(String pointer) {
        int slash = pointer.lastIndexOf('/');
        String seg = slash >= 0 ? pointer.substring(slash + 1) : pointer;
        return seg.replace("~1", "/").replace("~0", "~");
    }

    public static String toJavaAccessorStem(String cobolName) {
        StringBuilder sb = new StringBuilder();
        for (String part : cobolName.replace('.', ' ').replace('-', ' ').trim().split("\\s+")) {
            if (part.isBlank()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)))
              .append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    public static String toJavaFieldName(String cobolName) {
        String stem = toJavaAccessorStem(cobolName);
        return stem.isEmpty() ? stem : Character.toLowerCase(stem.charAt(0)) + stem.substring(1);
    }

    public static String toJavaMethodName(String cobolName) {
        return toJavaFieldName(cobolName);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -pl cobol-openrewrite-recipes test -o -Dtest=NodeIdentityIndexTest`
Expected: PASS (3 tests). Fix casing helpers if the recipe's existing `toPascal` differs (compare `PopulateCobolProcessRecipe.toPascal`).

- [ ] **Step 5: Commit**

```bash
git add cobol-openrewrite-recipes/src/main/java/org/shark/renovatio/cobol/recipes/annotate/NodeIdentityIndex.java \
        cobol-openrewrite-recipes/src/test/java/org/shark/renovatio/cobol/recipes/annotate/NodeIdentityIndexTest.java
git commit -m "feat(cobol): add NodeIdentityIndex resolving annotation nodeId to Java identifier"
```

---

## Task 3: `AnnotationApplicator` — eligibility filter, no mutation yet

**Files:**
- Create: `cobol-openrewrite-recipes/.../annotate/AnnotationApplicationOutcome.java`
- Create: `cobol-openrewrite-recipes/.../annotate/DroppedAnnotation.java`
- Create: `cobol-openrewrite-recipes/.../annotate/AnnotationApplicator.java`
- Test: `cobol-openrewrite-recipes/.../annotate/AnnotationApplicatorEligibilityTest.java`
- Add pom dep: `cobol-openrewrite-recipes/pom.xml` → `renovatio-cobol-annotations` (compile)

**Interfaces:**
- Consumes: `AnnotatedCobolContext` (`baseModel()`, `sidecar()`), `AnnotatedCobolModel` (`baseIrHash()`, `annotations()`), `CobolAnnotation` (`nodeId()`, `nodeKind()`, `annotationFamily()`, `payload()`, `review()`), `AnnotationReview.ReviewState`, `AnnotatedCobolValidator`, `CobolIrIdentityProjector`, `NodeIdentityIndex`.
- Produces:
  - `AnnotationApplicator(CobolIntermediateModel model, AnnotatedCobolModel sidecar)`.
  - `AnnotationApplicationOutcome apply(J.CompilationUnit cu, ExecutionContext ctx)`.
  - `record AnnotationApplicationOutcome(J.CompilationUnit tree, List<DroppedAnnotation> dropped)`.
  - `record DroppedAnnotation(String nodeId, String annotationId, AnnotationFamily family, DropReason reason, String detail)`; `enum DropReason { REJECTED, PENDING_REVIEW, STALE_SIDECAR, NAME_COLLISION, NODE_UNRESOLVED, FAMILY_NOT_APPLIED }`.
  - `List<CobolAnnotation> eligible()` — package-visible for tests: `ACCEPTED` + resolvable + kind match + hash match.

- [ ] **Step 1: Add the pom dependency**

In `cobol-openrewrite-recipes/pom.xml`, after the `renovatio-cobol-ir` dependency:

```xml
        <dependency>
            <groupId>org.shark.renovatio</groupId>
            <artifactId>renovatio-cobol-annotations</artifactId>
        </dependency>
```

- [ ] **Step 2: Write the failing test**

```java
package org.shark.renovatio.cobol.recipes.annotate;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.annotated.*;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.cobol.ir.parser.SimpleCobolIrParser;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationApplicatorEligibilityTest {

    private static final String COBOL = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. SAMPLE.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 CUSTOMER-NAME PIC X(30).
            PROCEDURE DIVISION.
            MAIN-PARA.
                MOVE 'A' TO CUSTOMER-NAME.
            """;

    private CobolIntermediateModel model() { return new SimpleCobolIrParser().parse(COBOL); }

    private CobolAnnotation domainNaming(CobolIntermediateModel model, AnnotationReview.ReviewState state) {
        CobolIrIdentityProjector p = new CobolIrIdentityProjector();
        String nodeId = p.nodes(model).stream()
                .filter(n -> n.nodeKind() == AnnotatedNodeKind.DATA_ITEM)
                .findFirst().orElseThrow().nodeId();
        AnnotationReview review = switch (state) {
            case ACCEPTED, REJECTED -> new AnnotationReview(state, null, "reviewer", Instant.parse("2026-01-01T00:00:00Z"));
            case NEEDS_REVIEW -> new AnnotationReview(state, "reviewer", null, null);
            case PROPOSED -> new AnnotationReview(state, null, null, null);
        };
        return new CobolAnnotation("a".repeat(64), nodeId, AnnotatedNodeKind.DATA_ITEM,
                AnnotationFamily.DOMAIN_NAMING,
                new DomainNamingPayload("clientFullName", "Customers", "rename for clarity"),
                0.9, provenance(), review);
    }

    // provenance(): build a minimal valid AnnotationProvenance — copy the shape from an existing
    // annotated-ir fixture test in renovatio-cobol-ir if this constructor is verbose.

    @Test
    void acceptsOnlyAcceptedAnnotationsWithMatchingHash() {
        CobolIntermediateModel model = model();
        String hash = new CobolIrIdentityProjector().baseIrHash(model);
        AnnotatedCobolModel sidecar = new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION,
                "cobol-ir.v1", hash, List.of(domainNaming(model, AnnotationReview.ReviewState.ACCEPTED)));

        AnnotationApplicator applicator = new AnnotationApplicator(model, sidecar);

        assertThat(applicator.eligible()).hasSize(1);
    }

    @Test
    void rejectsPendingProposedAndStaleHash() {
        CobolIntermediateModel model = model();
        AnnotatedCobolModel staleHash = new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION,
                "cobol-ir.v1", "0".repeat(64),
                List.of(domainNaming(model, AnnotationReview.ReviewState.ACCEPTED)));
        assertThat(new AnnotationApplicator(model, staleHash).eligible()).isEmpty();

        String hash = new CobolIrIdentityProjector().baseIrHash(model);
        AnnotatedCobolModel pending = new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION,
                "cobol-ir.v1", hash,
                List.of(domainNaming(model, AnnotationReview.ReviewState.NEEDS_REVIEW)));
        assertThat(new AnnotationApplicator(model, pending).eligible()).isEmpty();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn -q -pl cobol-openrewrite-recipes -am test -o -Dtest=AnnotationApplicatorEligibilityTest`
Expected: FAIL — `AnnotationApplicator` missing.

- [ ] **Step 4: Implement `DroppedAnnotation`, `AnnotationApplicationOutcome`, and `AnnotationApplicator.eligible()`**

`DroppedAnnotation.java`:

```java
package org.shark.renovatio.cobol.recipes.annotate;

import org.shark.renovatio.cobol.ir.annotated.AnnotationFamily;

public record DroppedAnnotation(String nodeId, String annotationId,
                                AnnotationFamily family, DropReason reason, String detail) {

    public enum DropReason {
        REJECTED, PENDING_REVIEW, STALE_SIDECAR, NAME_COLLISION, NODE_UNRESOLVED, FAMILY_NOT_APPLIED
    }
}
```

`AnnotationApplicationOutcome.java`:

```java
package org.shark.renovatio.cobol.recipes.annotate;

import org.openrewrite.java.tree.J;

import java.util.List;

public record AnnotationApplicationOutcome(J.CompilationUnit tree, List<DroppedAnnotation> dropped) {}
```

`AnnotationApplicator.java` (this step: constructor + `eligible()` only; `apply()` returns the tree unchanged plus every non-eligible annotation as a `DroppedAnnotation`):

```java
package org.shark.renovatio.cobol.recipes.annotate;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.shark.renovatio.cobol.ir.annotated.*;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AnnotationApplicator {

    private final CobolIntermediateModel model;
    private final AnnotatedCobolModel sidecar;
    private final NodeIdentityIndex index;
    private final boolean hashMatches;

    public AnnotationApplicator(CobolIntermediateModel model, AnnotatedCobolModel sidecar) {
        this.model = model;
        this.sidecar = sidecar;
        this.index = new NodeIdentityIndex(model);
        this.hashMatches = new CobolIrIdentityProjector().baseIrHash(model).equals(sidecar.baseIrHash());
    }

    List<CobolAnnotation> ordered() {
        List<CobolAnnotation> list = new ArrayList<>(sidecar.annotations());
        list.sort(Comparator.comparing(CobolAnnotation::nodeId).thenComparing(CobolAnnotation::annotationId));
        return list;
    }

    List<CobolAnnotation> eligible() {
        List<CobolAnnotation> out = new ArrayList<>();
        if (!hashMatches) {
            return out;
        }
        for (CobolAnnotation a : ordered()) {
            if (a.review().reviewState() != AnnotationReview.ReviewState.ACCEPTED) continue;
            if (a.annotationFamily() != AnnotationFamily.DOMAIN_NAMING
                    && a.annotationFamily() != AnnotationFamily.DATA_INTENT) continue;
            if (index.resolve(a.nodeId(), a.nodeKind()).isEmpty()) continue;
            out.add(a);
        }
        return out;
    }

    public AnnotationApplicationOutcome apply(J.CompilationUnit cu, ExecutionContext ctx) {
        List<DroppedAnnotation> dropped = new ArrayList<>();
        for (CobolAnnotation a : ordered()) {
            classifyDrop(a).ifPresent(dropped::add);
        }
        // Tasks 4-5 add real mutation here; for now return the tree unchanged.
        return new AnnotationApplicationOutcome(cu, dropped);
    }

    private java.util.Optional<DroppedAnnotation> classifyDrop(CobolAnnotation a) {
        DroppedAnnotation.DropReason reason;
        if (!hashMatches) {
            reason = DroppedAnnotation.DropReason.STALE_SIDECAR;
        } else if (a.annotationFamily() == AnnotationFamily.CONTROL_FLOW_PLAN
                || a.annotationFamily() == AnnotationFamily.UNSUPPORTED_EXPLANATION) {
            reason = DroppedAnnotation.DropReason.FAMILY_NOT_APPLIED;
        } else switch (a.review().reviewState()) {
            case REJECTED -> reason = DroppedAnnotation.DropReason.REJECTED;
            case PROPOSED, NEEDS_REVIEW -> reason = DroppedAnnotation.DropReason.PENDING_REVIEW;
            case ACCEPTED -> {
                if (index.resolve(a.nodeId(), a.nodeKind()).isEmpty()) {
                    reason = DroppedAnnotation.DropReason.NODE_UNRESOLVED;
                } else {
                    return java.util.Optional.empty(); // eligible; not dropped here
                }
            }
            default -> { return java.util.Optional.empty(); }
        }
        return java.util.Optional.of(new DroppedAnnotation(a.nodeId(), a.annotationId(),
                a.annotationFamily(), reason, a.annotationFamily().name()));
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn -q -pl cobol-openrewrite-recipes -am test -o -Dtest=AnnotationApplicatorEligibilityTest`
Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git add cobol-openrewrite-recipes/pom.xml cobol-openrewrite-recipes/src/main/java/org/shark/renovatio/cobol/recipes/annotate/ \
        cobol-openrewrite-recipes/src/test/java/org/shark/renovatio/cobol/recipes/annotate/AnnotationApplicatorEligibilityTest.java
git commit -m "feat(cobol): AnnotationApplicator eligibility filtering for annotated IR pass"
```

---

## Task 4: `AnnotationApplicator` — apply `DATA_INTENT` marker

**Files:**
- Modify: `cobol-openrewrite-recipes/.../annotate/AnnotationApplicator.java`
- Test: `cobol-openrewrite-recipes/.../annotate/AnnotationApplicatorDataIntentTest.java`

**Interfaces:**
- Consumes: `DataIntentPayload` (`construction()` → enum `REDEFINES`/`OCCURS_DEPENDING_ON`, `interpretation()`, `assumptions()`), `org.openrewrite.java.tree.J`, `JavaTemplate`, `org.openrewrite.java.JavaIsoVisitor`.
- Produces: `apply()` now attaches `@org.shark.renovatio.cobol.annotations.CobolDataIntent(...)` to the matching `J.VariableDeclarations` (field) whose variable name equals `NodeIdentityIndex.toJavaFieldName(resolved.cobolName())`.

- [ ] **Step 1: Write the failing test**

```java
package org.shark.renovatio.cobol.recipes.annotate;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationApplicatorDataIntentTest {

    private static final String DTO = """
            package sample;
            public class SampleDto {
                private String customerName;
                public String getCustomerName() { return customerName; }
                public void setCustomerName(String v) { this.customerName = v; }
            }
            """;

    @Test
    void attachesCobolDataIntentAnnotationToField() {
        var ctx = new InMemoryExecutionContext();
        J.CompilationUnit cu = JavaParser.fromJavaVersion().build()
                .parse(ctx, DTO).map(J.CompilationUnit.class::cast).findFirst().orElseThrow();

        // Build model + sidecar with an ACCEPTED DATA_INTENT annotation on CUSTOMER-NAME (REDEFINES).
        // Reuse the helper from AnnotationApplicatorEligibilityTest (extract to a shared test fixture
        // class AnnotatedFixtures under src/test/java/.../annotate/).
        AnnotatedFixtures f = AnnotatedFixtures.redefinesDataIntent();

        AnnotationApplicationOutcome outcome =
                new AnnotationApplicator(f.model(), f.sidecar()).apply(cu, ctx);

        String printed = outcome.tree().printAll();
        assertThat(printed).contains("@CobolDataIntent(");
        assertThat(printed).contains("construction = CobolDataIntent.Construction.REDEFINES");
        assertThat(printed).contains("private String customerName");
        assertThat(outcome.dropped()).isEmpty();
    }

    @Test
    void isDeterministicAcrossRuns() {
        var ctx1 = new InMemoryExecutionContext();
        var ctx2 = new InMemoryExecutionContext();
        AnnotatedFixtures f = AnnotatedFixtures.redefinesDataIntent();
        J.CompilationUnit cu1 = parse(ctx1), cu2 = parse(ctx2);

        String a = new AnnotationApplicator(f.model(), f.sidecar()).apply(cu1, ctx1).tree().printAll();
        String b = new AnnotationApplicator(f.model(), f.sidecar()).apply(cu2, ctx2).tree().printAll();
        assertThat(a).isEqualTo(b);
    }

    private J.CompilationUnit parse(org.openrewrite.ExecutionContext ctx) {
        return JavaParser.fromJavaVersion().build()
                .parse(ctx, DTO).map(J.CompilationUnit.class::cast).findFirst().orElseThrow();
    }
}
```

- [ ] **Step 2: Create the shared test fixture helper**

`AnnotatedFixtures.java` (test source, `src/test/java/org/shark/renovatio/cobol/recipes/annotate/`): a small builder returning `record Fixture(CobolIntermediateModel model, AnnotatedCobolModel sidecar)` with factory methods `redefinesDataIntent()` and `domainNaming(ReviewState)`. Move the annotation/provenance/review construction out of `AnnotationApplicatorEligibilityTest` into this class and update that test to use it.

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn -q -pl cobol-openrewrite-recipes test -o -Dtest=AnnotationApplicatorDataIntentTest`
Expected: FAIL — no `@CobolDataIntent` in output.

- [ ] **Step 4: Implement the marker application**

In `AnnotationApplicator.apply()`, replace the "Tasks 4-5 add real mutation here" comment with a visitor pass. For each eligible `DATA_INTENT` annotation, resolve the field name and run:

```java
private J.CompilationUnit applyDataIntent(J.CompilationUnit cu, ExecutionContext ctx,
                                          CobolAnnotation a, String fieldName) {
    DataIntentPayload payload = (DataIntentPayload) a.payload();
    String assumptionsArray = payload.assumptions().stream()
            .map(s -> '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"')
            .collect(java.util.stream.Collectors.joining(", ", "{", "}"));
    String tmpl = String.format(java.util.Locale.ROOT,
            "@CobolDataIntent(nodeId = \"%s\", annotationId = \"%s\", "
          + "construction = CobolDataIntent.Construction.%s, interpretation = \"%s\", assumptions = %s)",
            a.nodeId(), a.annotationId(), payload.construction().name(),
            payload.interpretation().replace("\"", "\\\""), assumptionsArray);

    return (J.CompilationUnit) new org.openrewrite.java.JavaIsoVisitor<ExecutionContext>() {
        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations v, ExecutionContext c) {
            J.VariableDeclarations vd = super.visitVariableDeclarations(v, c);
            boolean isField = getCursor().firstEnclosing(J.ClassDeclaration.class) != null
                    && getCursor().firstEnclosing(J.MethodDeclaration.class) == null;
            if (isField && !vd.getVariables().isEmpty()
                    && vd.getVariables().get(0).getSimpleName().equals(fieldName)
                    && vd.getLeadingAnnotations().stream().noneMatch(an -> "CobolDataIntent".equals(an.getSimpleName()))) {
                return JavaTemplate.builder(tmpl)
                        .imports("org.shark.renovatio.cobol.annotations.CobolDataIntent")
                        .javaParser(JavaParser.fromJavaVersion()
                            .classpath(org.openrewrite.java.JavaParser.runtimeClasspath()))
                        .build()
                        .apply(getCursor(), vd.getCoordinates().addAnnotation(
                            java.util.Comparator.comparing(J.Annotation::getSimpleName)));
            }
            return vd;
        }
    }.visitCompilationUnit(cu, ctx);
}
```

> `JavaTemplate` needs `renovatio-cobol-annotations` on the parser classpath. If `JavaParser.runtimeClasspath()` does not pick it up in the recipes-module test JVM, add `rewrite-java-17` (already test-scoped) and pass `.classpath("renovatio-cobol-annotations")` or the jar path resolved from the test classpath. Also add the import via `maybeAddImport("org.shark.renovatio.cobol.annotations.CobolDataIntent")` in the visitor and run `doAfterVisit(new org.openrewrite.java.AddImport<>(...))` if the template does not.

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn -q -pl cobol-openrewrite-recipes test -o -Dtest=AnnotationApplicatorDataIntentTest`
Expected: PASS (2 tests). Iterate on the `JavaTemplate` classpath wiring until the annotation renders and the import resolves.

- [ ] **Step 6: Commit**

```bash
git add cobol-openrewrite-recipes/src/main/java/org/shark/renovatio/cobol/recipes/annotate/AnnotationApplicator.java \
        cobol-openrewrite-recipes/src/test/java/org/shark/renovatio/cobol/recipes/annotate/
git commit -m "feat(cobol): apply DATA_INTENT annotations as @CobolDataIntent markers"
```

---

## Task 5: `AnnotationApplicator` — apply `DOMAIN_NAMING` rename with collision drop

**Files:**
- Modify: `cobol-openrewrite-recipes/.../annotate/AnnotationApplicator.java`
- Test: `cobol-openrewrite-recipes/.../annotate/AnnotationApplicatorDomainNamingTest.java`

**Interfaces:**
- Consumes: `DomainNamingPayload` (`suggestedName()`, `boundedContext()`, `rationale()`), OpenRewrite `org.openrewrite.java.ChangeFieldName`, `org.openrewrite.java.tree.J`, a scope-collision scan.
- Produces: `apply()` renames the private field + getter + setter + uses for a `DATA_ITEM` node; renames the service method for a `PARAGRAPH` node; on collision emits `DroppedAnnotation(NAME_COLLISION)` and leaves the tree unchanged for that annotation.

- [ ] **Step 1: Write the failing test**

```java
package org.shark.renovatio.cobol.recipes.annotate;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationApplicatorDomainNamingTest {

    private static final String DTO = """
            package sample;
            public class SampleDto {
                private String customerName;
                public String getCustomerName() { return customerName; }
                public void setCustomerName(String v) { this.customerName = v; }
            }
            """;

    @Test
    void renamesFieldAndAccessors() {
        var ctx = new InMemoryExecutionContext();
        J.CompilationUnit cu = parse(ctx);
        AnnotatedFixtures f = AnnotatedFixtures.domainNaming("clientFullName"); // ACCEPTED, on CUSTOMER-NAME

        String printed = new AnnotationApplicator(f.model(), f.sidecar()).apply(cu, ctx).tree().printAll();

        assertThat(printed).contains("private String clientFullName");
        assertThat(printed).contains("public String getClientFullName()");
        assertThat(printed).contains("public void setClientFullName(");
        assertThat(printed).doesNotContain("customerName");
    }

    @Test
    void dropsRenameOnCollisionAndLeavesSourceUnchanged() {
        var ctx = new InMemoryExecutionContext();
        String dtoWithClash = DTO.replace("private String customerName;",
                "private String customerName;\n    private String clientFullName;");
        J.CompilationUnit cu = JavaParser.fromJavaVersion().build()
                .parse(ctx, dtoWithClash).map(J.CompilationUnit.class::cast).findFirst().orElseThrow();
        AnnotatedFixtures f = AnnotatedFixtures.domainNaming("clientFullName");

        AnnotationApplicationOutcome outcome = new AnnotationApplicator(f.model(), f.sidecar()).apply(cu, ctx);

        assertThat(outcome.tree().printAll()).contains("private String customerName");
        assertThat(outcome.dropped())
                .anySatisfy(d -> assertThat(d.reason()).isEqualTo(DroppedAnnotation.DropReason.NAME_COLLISION));
    }

    private J.CompilationUnit parse(org.openrewrite.ExecutionContext ctx) {
        return JavaParser.fromJavaVersion().build()
                .parse(ctx, DTO).map(J.CompilationUnit.class::cast).findFirst().orElseThrow();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl cobol-openrewrite-recipes test -o -Dtest=AnnotationApplicatorDomainNamingTest`
Expected: FAIL — rename not performed.

- [ ] **Step 3: Implement the rename + collision scan**

Add to `AnnotationApplicator`:

```java
private static final java.util.regex.Pattern IDENT =
        java.util.regex.Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");

private boolean identifierInUse(J.CompilationUnit cu, String candidate) {
    java.util.concurrent.atomic.AtomicBoolean found = new java.util.concurrent.atomic.AtomicBoolean(false);
    new org.openrewrite.java.JavaIsoVisitor<Integer>() {
        @Override public J.Identifier visitIdentifier(J.Identifier ident, Integer p) {
            if (ident.getSimpleName().equals(candidate)) found.set(true);
            return ident;
        }
    }.visit(cu, 0);
    return found.get();
}

private J.CompilationUnit applyDomainNaming(J.CompilationUnit cu, ExecutionContext ctx,
                                            CobolAnnotation a, String currentField,
                                            java.util.List<DroppedAnnotation> dropped) {
    DomainNamingPayload payload = (DomainNamingPayload) a.payload();
    String target = payload.suggestedName();
    if (!IDENT.matcher(target).matches() || identifierInUse(cu, target)) {
        dropped.add(new DroppedAnnotation(a.nodeId(), a.annotationId(), a.annotationFamily(),
                DroppedAnnotation.DropReason.NAME_COLLISION, payload.rationale()));
        return cu;
    }
    String stem = NodeIdentityIndex.toJavaAccessorStem(currentField); // "CustomerName"
    String newStem = Character.toUpperCase(target.charAt(0)) + target.substring(1);
    J.CompilationUnit out = cu;
    out = (J.CompilationUnit) new org.openrewrite.java.ChangeFieldName<>(
            "sample.SampleDto", currentField, target).getVisitor().visit(out, ctx); // FQN from cu
    out = renameMethods(out, ctx, "get" + stem, "get" + newStem);
    out = renameMethods(out, ctx, "set" + stem, "set" + newStem);
    return out;
}
```

> `ChangeFieldName` needs the declaring type's fully-qualified name — read it from the `J.ClassDeclaration` in `cu` rather than hard-coding `sample.SampleDto`. `renameMethods` is a small `JavaIsoVisitor` swapping `J.MethodDeclaration.name` and matching `J.MethodInvocation` simple names. For a `PARAGRAPH` node, rename the service method instead (same `renameMethods` helper on the service class).

Wire `apply()` to call `applyDataIntent` / `applyDomainNaming` per eligible annotation in `(nodeId, annotationId)` order, threading the evolving `J.CompilationUnit`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -pl cobol-openrewrite-recipes test -o -Dtest=AnnotationApplicatorDomainNamingTest`
Expected: PASS (2 tests).

- [ ] **Step 5: Run the full recipes module suite**

Run: `mvn -q -pl cobol-openrewrite-recipes test -o`
Expected: PASS (all existing + new).

- [ ] **Step 6: Commit**

```bash
git add cobol-openrewrite-recipes/src/main/java/org/shark/renovatio/cobol/recipes/annotate/AnnotationApplicator.java \
        cobol-openrewrite-recipes/src/test/java/org/shark/renovatio/cobol/recipes/annotate/AnnotationApplicatorDomainNamingTest.java
git commit -m "feat(cobol): apply DOMAIN_NAMING renames with deterministic collision drop"
```

---

## Task 6: Wire `AnnotationApplicator` into `PopulateCobolProcessRecipe`

**Files:**
- Modify: `cobol-openrewrite-recipes/.../PopulateCobolProcessRecipe.java`
- Create: `cobol-openrewrite-recipes/.../annotate/AnnotationOutcomeKey.java`
- Test: `cobol-openrewrite-recipes/.../PopulateCobolProcessRecipeAnnotatedTest.java`

**Interfaces:**
- Consumes: `AnnotatedCobolContext` from `ctx.getMessage(ANNOTATED_CONTEXT_KEY)`, `AnnotationApplicator`.
- Produces: `public static final String ANNOTATION_OUTCOMES_KEY = "renovatio.cobol.annotation-outcomes"` on `AnnotationOutcomeKey`; after recipe execution the `ExecutionContext` holds a `List<DroppedAnnotation>` (possibly empty) under that key. The generated method body is unchanged from today's deterministic output when no eligible annotation applies.

- [ ] **Step 1: Write the failing test**

```java
package org.shark.renovatio.cobol.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.SourceFile;
import org.shark.renovatio.cobol.recipes.annotate.AnnotationOutcomeKey;
import org.shark.renovatio.cobol.recipes.annotate.DroppedAnnotation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PopulateCobolProcessRecipeAnnotatedTest {

    @Test
    void appliesAcceptedRenameAndRecordsDroppedAnnotations() {
        var ctx = new InMemoryExecutionContext();
        AnnotatedFixturesRecipes f = AnnotatedFixturesRecipes.moveWithAcceptedRenameAndRejected();
        ctx.putMessage(PopulateCobolProcessRecipe.CONTEXT_KEY, f.model());
        ctx.putMessage(PopulateCobolProcessRecipe.ANNOTATED_CONTEXT_KEY, f.context());

        List<SourceFile> sources = JavaParser.fromJavaVersion().build()
                .parse(ctx, f.javaStub()).map(SourceFile.class::cast).toList();

        var run = new PopulateCobolProcessRecipe().run(
                new org.openrewrite.internal.InMemoryLargeSourceSet(sources), ctx);

        String after = run.getChangeset().getAllResults().get(0).getAfter().printAll();
        assertThat(after).contains(f.expectedRenamedIdentifier());

        List<DroppedAnnotation> dropped = ctx.getMessage(AnnotationOutcomeKey.ANNOTATION_OUTCOMES_KEY);
        assertThat(dropped).isNotNull();
        assertThat(dropped).anySatisfy(d ->
                assertThat(d.reason()).isEqualTo(DroppedAnnotation.DropReason.REJECTED));
    }

    @Test
    void legacyPathUnchangedWhenNoAnnotatedContext() {
        var ctx = new InMemoryExecutionContext();
        AnnotatedFixturesRecipes f = AnnotatedFixturesRecipes.moveWithAcceptedRenameAndRejected();
        ctx.putMessage(PopulateCobolProcessRecipe.CONTEXT_KEY, f.model());
        // no ANNOTATED_CONTEXT_KEY

        List<SourceFile> sources = JavaParser.fromJavaVersion().build()
                .parse(ctx, f.javaStub()).map(SourceFile.class::cast).toList();
        var run = new PopulateCobolProcessRecipe().run(
                new org.openrewrite.internal.InMemoryLargeSourceSet(sources), ctx);

        assertThat(run.getChangeset().getAllResults().get(0).getAfter().printAll())
                .isEqualTo(f.expectedLegacyOutput());
        assertThat(ctx.<Object>getMessage(AnnotationOutcomeKey.ANNOTATION_OUTCOMES_KEY)).isNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl cobol-openrewrite-recipes test -o -Dtest=PopulateCobolProcessRecipeAnnotatedTest`
Expected: FAIL — no outcome key set; rename not applied.

- [ ] **Step 3: Implement `AnnotationOutcomeKey` and wire the recipe**

`AnnotationOutcomeKey.java`:

```java
package org.shark.renovatio.cobol.recipes.annotate;

public final class AnnotationOutcomeKey {
    public static final String ANNOTATION_OUTCOMES_KEY = "renovatio.cobol.annotation-outcomes";
    private AnnotationOutcomeKey() {}
}
```

In `PopulateCobolProcessRecipe.getVisitor()`, wrap the existing `PopulateVisitor` so that after it runs on a `J.CompilationUnit`, an `AnnotationApplicator` post-pass runs once per compilation unit. Simplest: override `visitCompilationUnit`:

```java
@Override
public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
    J.CompilationUnit populated = super.visitCompilationUnit(cu, ctx);
    AnnotatedCobolContext annotated = ctx.getMessage(ANNOTATED_CONTEXT_KEY);
    CobolIntermediateModel model = resolveModel(ctx);
    if (annotated == null || model == null || annotated.baseModel() != model) {
        return populated;
    }
    AnnotationApplicator applicator = new AnnotationApplicator(model, annotated.sidecar());
    AnnotationApplicationOutcome outcome = applicator.apply(populated, ctx);
    List<DroppedAnnotation> acc = ctx.getMessage(AnnotationOutcomeKey.ANNOTATION_OUTCOMES_KEY);
    if (acc == null) { acc = new ArrayList<>(); ctx.putMessage(AnnotationOutcomeKey.ANNOTATION_OUTCOMES_KEY, acc); }
    acc.addAll(outcome.dropped());
    return outcome.tree();
}
```

Keep `resolveModel` and `PopulateVisitor` as-is.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -pl cobol-openrewrite-recipes test -o -Dtest=PopulateCobolProcessRecipeAnnotatedTest,PopulateCobolProcessRecipeTest`
Expected: PASS. The existing `PopulateCobolProcessRecipeTest` must be unchanged (legacy path identical).

- [ ] **Step 5: Commit**

```bash
git add cobol-openrewrite-recipes/src/main/java/org/shark/renovatio/cobol/recipes/
git add cobol-openrewrite-recipes/src/test/java/org/shark/renovatio/cobol/recipes/PopulateCobolProcessRecipeAnnotatedTest.java
git commit -m "feat(cobol): run AnnotationApplicator post-pass inside PopulateCobolProcessRecipe"
```

---

## Task 7: `AnnotatedContextResolver` in `renovatio-provider-cobol`

**Files:**
- Create: `renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/translation/AnnotatedContextResolver.java`
- Test: `renovatio-provider-cobol/src/test/java/org/shark/renovatio/provider/cobol/translation/AnnotatedContextResolverTest.java`

**Interfaces:**
- Consumes: `CobolIntermediateModel`, `AnnotatedCobolModel`, `AnnotatedCobolContext`, `AnnotatedCobolValidator`, `CobolIrIdentityProjector`, `GuardrailSchemaCatalog` (`resolve("cobol-annotated-ir.v1")`), Jackson `ObjectMapper`, `com.networknt` schema validator.
- Produces:
  - `AnnotatedContextResolver(ObjectMapper mapper)`.
  - `record Request(Optional<AnnotatedCobolModel> inlineSidecar, Optional<Path> sidecarPath, Path cobolSourcePath)`.
  - `Resolution resolve(Request request, CobolIntermediateModel model)` where `record Resolution(Optional<AnnotatedCobolContext> context, List<String> diagnostics)`.
  - Precedence: inline sidecar → explicit `sidecarPath` → `<cobolSourcePath-stem>.annotated.json` sibling → empty.
  - A sidecar is accepted only if: JSON parses, passes `cobol-annotated-ir.v1` schema, passes `AnnotatedCobolValidator` with zero diagnostics, and `baseIrHash` matches `projector.baseIrHash(model)`. Otherwise it is skipped (with a diagnostic string) and resolution falls through.

- [ ] **Step 1: Write the failing test**

```java
package org.shark.renovatio.provider.cobol.translation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;
import org.shark.renovatio.cobol.ir.annotated.CobolIrIdentityProjector;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotatedContextResolverTest {

    private static final String COBOL = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. SAMPLE.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 CUSTOMER-NAME PIC X(30).
            PROCEDURE DIVISION.
            MAIN-PARA.
                MOVE 'A' TO CUSTOMER-NAME.
            """;

    private CobolIntermediateModel model() {
        return new CobolIntermediateModelService().parse(COBOL);
    }

    @Test
    void prefersInlineSidecarOverPath(@TempDir Path dir) throws Exception {
        CobolIntermediateModel model = model();
        String hash = new CobolIrIdentityProjector().baseIrHash(model);
        AnnotatedCobolModel inline = new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION,
                "cobol-ir.v1", hash, List.of());
        Path cob = dir.resolve("SAMPLE.cob");
        Files.writeString(cob, COBOL);

        var resolver = new AnnotatedContextResolver(new ObjectMapper());
        var resolution = resolver.resolve(
                new AnnotatedContextResolver.Request(Optional.of(inline), Optional.empty(), cob), model);

        assertThat(resolution.context()).isPresent();
        assertThat(resolution.context().get().sidecar()).isSameAs(inline);
    }

    @Test
    void fallsThroughToLegacyOnStalePathSidecar(@TempDir Path dir) throws Exception {
        CobolIntermediateModel model = model();
        Path cob = dir.resolve("SAMPLE.cob");
        Files.writeString(cob, COBOL);
        Files.writeString(dir.resolve("SAMPLE.annotated.json"),
                "{\"schemaVersion\":\"cobol-annotated-ir.v1\",\"baseIrVersion\":\"cobol-ir.v1\","
              + "\"baseIrHash\":\"" + "0".repeat(64) + "\",\"annotations\":[]}");

        var resolution = new AnnotatedContextResolver(new ObjectMapper()).resolve(
                new AnnotatedContextResolver.Request(Optional.empty(), Optional.empty(), cob), model);

        assertThat(resolution.context()).isEmpty();
        assertThat(resolution.diagnostics()).anyMatch(d -> d.contains("baseIrHash"));
    }

    @Test
    void usesCommittedPathSidecarWhenValid(@TempDir Path dir) throws Exception {
        CobolIntermediateModel model = model();
        String hash = new CobolIrIdentityProjector().baseIrHash(model);
        Path cob = dir.resolve("SAMPLE.cob");
        Files.writeString(cob, COBOL);
        Files.writeString(dir.resolve("SAMPLE.annotated.json"),
                "{\"schemaVersion\":\"cobol-annotated-ir.v1\",\"baseIrVersion\":\"cobol-ir.v1\","
              + "\"baseIrHash\":\"" + hash + "\",\"annotations\":[]}");

        var resolution = new AnnotatedContextResolver(new ObjectMapper()).resolve(
                new AnnotatedContextResolver.Request(Optional.empty(), Optional.empty(), cob), model);

        assertThat(resolution.context()).isPresent();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl renovatio-provider-cobol test -o -Dtest=AnnotatedContextResolverTest`
Expected: FAIL — `AnnotatedContextResolver` missing.

- [ ] **Step 3: Implement `AnnotatedContextResolver`**

Model the JSON→model deserialization and schema validation on the existing `CobolSemanticTranspiler.isValid()` and `GuardrailSchemaCatalogTest`. Reuse `new GuardrailSchemaCatalog(mapper).resolve("cobol-annotated-ir.v1")` for the schema node, `JsonSchemaFactory.getInstance(V202012)` for validation, and `AnnotatedCobolValidator` for semantic checks (the same call `CobolSemanticTranspiler.isValid` already performs). Build `AnnotatedCobolContext` only when all checks pass and `projector.baseIrHash(model).equals(sidecar.baseIrHash())`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -pl renovatio-provider-cobol test -o -Dtest=AnnotatedContextResolverTest`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/translation/AnnotatedContextResolver.java \
        renovatio-provider-cobol/src/test/java/org/shark/renovatio/provider/cobol/translation/AnnotatedContextResolverTest.java
git commit -m "feat(cobol): AnnotatedContextResolver with request/path/legacy precedence"
```

---

## Task 8: `AnnotationActionItemFactory` + `CobolSemanticTranspiler` drain

**Files:**
- Create: `renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/translation/AnnotationActionItemFactory.java`
- Modify: `renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/translation/CobolSemanticTranspiler.java`
- Test: `renovatio-provider-cobol/src/test/java/org/shark/renovatio/provider/cobol/translation/AnnotationActionItemFactoryTest.java`
- Modify test: `CobolSemanticTranspilerTest.java` — add a drain assertion

**Interfaces:**
- Consumes: `DroppedAnnotation` (from `cobol-openrewrite-recipes`; add that module as a compile dep of `renovatio-provider-cobol` — it already depends on it transitively via recipes, confirm scope), `ManualActionItem`, `ManualActionItemIds`, `GuardrailGate`, `ManualActionSeverity`, `ManualActionReviewStatus`.
- Produces:
  - `AnnotationActionItemFactory` with `ManualActionItem toActionItem(DroppedAnnotation dropped, String sourceFile, String programId)`.
  - Mapping table (spec §6):
    | `DropReason` | `failedGate` | `severity` | `diagnosticReference` |
    | --- | --- | --- | --- |
    | `REJECTED` | `REVIEW_ELIGIBILITY` | `WARNING` | `COBOL-ANNOTATION-REJECTED` |
    | `PENDING_REVIEW` | `REVIEW_ELIGIBILITY` | `WARNING` | `COBOL-ANNOTATION-PENDING` |
    | `STALE_SIDECAR` | `CHARACTERIZATION` | `ERROR` | `COBOL-ANNOTATION-STALE` |
    | `NAME_COLLISION` | `REVIEW_ELIGIBILITY` | `WARNING` | `COBOL-DOMAIN-RENAME-COLLISION` |
    | `NODE_UNRESOLVED` | `REVIEW_ELIGIBILITY` | `ERROR` | `COBOL-ANNOTATION-NODE-UNRESOLVED` |
    | `FAMILY_NOT_APPLIED` | `REVIEW_ELIGIBILITY` | `WARNING` | `COBOL-ANNOTATION-FAMILY-DEFERRED` |
  - `CobolSemanticTranspiler.enrichServiceImplementation(String javaSource, AnnotatedCobolContext ctx, java.util.function.Consumer<List<ManualActionItem>> sink)` — runs the recipe, drains `AnnotationOutcomeKey.ANNOTATION_OUTCOMES_KEY`, maps via the factory, and passes the list to `sink`. The existing two-arg and `AnnotatedCobolContext` overloads delegate with a no-op sink.

- [ ] **Step 1: Write the failing factory test**

```java
package org.shark.renovatio.provider.cobol.translation;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.annotated.AnnotationFamily;
import org.shark.renovatio.cobol.recipes.annotate.DroppedAnnotation;
import org.shark.renovatio.provider.cobol.guardrail.GuardrailGate;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionItem;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionSeverity;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationActionItemFactoryTest {

    private final AnnotationActionItemFactory factory = new AnnotationActionItemFactory();

    @Test
    void mapsRejectedToReviewEligibilityWarning() {
        DroppedAnnotation d = new DroppedAnnotation("n", "a", AnnotationFamily.DOMAIN_NAMING,
                DroppedAnnotation.DropReason.REJECTED, "rationale");

        ManualActionItem item = factory.toActionItem(d, "SAMPLE.cob", "SAMPLE");

        assertThat(item.id()).matches("^mai-[a-f0-9]{24}$");
        assertThat(item.failedGate()).isEqualTo(GuardrailGate.REVIEW_ELIGIBILITY);
        assertThat(item.severity()).isEqualTo(ManualActionSeverity.WARNING);
        assertThat(item.diagnosticReference()).isEqualTo("COBOL-ANNOTATION-REJECTED");
        assertThat(item.irNodeId()).isEqualTo("n");
    }

    @Test
    void mapsStaleSidecarToCharacterizationError() {
        DroppedAnnotation d = new DroppedAnnotation("n", "a", AnnotationFamily.DATA_INTENT,
                DroppedAnnotation.DropReason.STALE_SIDECAR, "DATA_INTENT");
        ManualActionItem item = factory.toActionItem(d, "SAMPLE.cob", "SAMPLE");
        assertThat(item.failedGate()).isEqualTo(GuardrailGate.CHARACTERIZATION);
        assertThat(item.severity()).isEqualTo(ManualActionSeverity.ERROR);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl renovatio-provider-cobol test -o -Dtest=AnnotationActionItemFactoryTest`
Expected: FAIL — factory missing.

- [ ] **Step 3: Implement `AnnotationActionItemFactory`**

```java
package org.shark.renovatio.provider.cobol.translation;

import org.shark.renovatio.cobol.recipes.annotate.DroppedAnnotation;
import org.shark.renovatio.provider.cobol.guardrail.*;

public final class AnnotationActionItemFactory {

    public ManualActionItem toActionItem(DroppedAnnotation d, String sourceFile, String programId) {
        Mapping m = switch (d.reason()) {
            case REJECTED -> new Mapping(GuardrailGate.REVIEW_ELIGIBILITY, ManualActionSeverity.WARNING,
                    "COBOL-ANNOTATION-REJECTED", "Rejected annotation not applied; deterministic translation retained");
            case PENDING_REVIEW -> new Mapping(GuardrailGate.REVIEW_ELIGIBILITY, ManualActionSeverity.WARNING,
                    "COBOL-ANNOTATION-PENDING", "Annotation pending human review; not eligible for deterministic application");
            case STALE_SIDECAR -> new Mapping(GuardrailGate.CHARACTERIZATION, ManualActionSeverity.ERROR,
                    "COBOL-ANNOTATION-STALE", "Annotated sidecar does not match current IR; regenerate the sidecar");
            case NAME_COLLISION -> new Mapping(GuardrailGate.REVIEW_ELIGIBILITY, ManualActionSeverity.WARNING,
                    "COBOL-DOMAIN-RENAME-COLLISION", "Domain rename collides with an existing identifier in scope");
            case NODE_UNRESOLVED -> new Mapping(GuardrailGate.REVIEW_ELIGIBILITY, ManualActionSeverity.ERROR,
                    "COBOL-ANNOTATION-NODE-UNRESOLVED", "Annotation node cannot be resolved against the current IR");
            case FAMILY_NOT_APPLIED -> new Mapping(GuardrailGate.REVIEW_ELIGIBILITY, ManualActionSeverity.WARNING,
                    "COBOL-ANNOTATION-FAMILY-DEFERRED", "Annotation family not applied deterministically; recorded for manual action");
        };
        String family = d.family().name();
        String id = ManualActionItemIds.from(sourceFile, programId, d.nodeId(), family, m.reason());
        return new ManualActionItem(id, sourceFile, programId, null, null, null, null,
                d.nodeId(), null, family, m.reason(), m.gate(), m.code(),
                "No annotated transformation applied for the affected node",
                "Human review of annotation " + d.annotationId(),
                "Annotation is accepted, matches current IR, and applies without collision",
                m.severity(), ManualActionReviewStatus.PENDING, null, null, null, null, null, null);
    }

    private record Mapping(GuardrailGate gate, ManualActionSeverity severity, String code, String reason) {}
}
```

- [ ] **Step 4: Run factory test to verify it passes**

Run: `mvn -q -pl renovatio-provider-cobol test -o -Dtest=AnnotationActionItemFactoryTest`
Expected: PASS (2 tests).

- [ ] **Step 5: Write the failing transpiler drain test**

Add to `CobolSemanticTranspilerTest.java`:

```java
@Test
void drainsDroppedAnnotationsToSink() {
    CobolIntermediateModel model = new CobolIntermediateModelService().parse(COBOL_SAMPLE);
    AnnotatedCobolModel sidecar = new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION,
            "cobol-ir.v1", new CobolIrIdentityProjector().baseIrHash(model),
            List.of(/* one REJECTED DOMAIN_NAMING annotation on CUSTOMER-NAME */
                    AnnotatedTestFixtures.rejectedDomainNaming(model)));
    AnnotatedCobolContext ctx = new AnnotatedCobolContext(model, sidecar);

    java.util.List<ManualActionItem> captured = new java.util.ArrayList<>();
    new CobolSemanticTranspiler(new OpenRewriteRunner())
            .enrichServiceImplementation(JAVA_STUB, ctx, captured::addAll);

    assertThat(captured).anySatisfy(i ->
            assertThat(i.diagnosticReference()).isEqualTo("COBOL-ANNOTATION-REJECTED"));
}
```

- [ ] **Step 6: Run to verify it fails, then implement the drain**

Run: `mvn -q -pl renovatio-provider-cobol test -o -Dtest=CobolSemanticTranspilerTest#drainsDroppedAnnotationsToSink`
Expected: FAIL — three-arg overload missing.

Add to `CobolSemanticTranspiler`: a private field `AnnotationActionItemFactory actionItemFactory = new AnnotationActionItemFactory();`, the three-arg `enrichServiceImplementation(String, AnnotatedCobolContext, Consumer<List<ManualActionItem>>)` that after `runner.runRecipe(...)` reads `ctx.getMessage("renovatio.cobol.annotation-outcomes")`, maps each `DroppedAnnotation` with the factory (source file / program id from `model.getProgramId()` and a caller-provided or `"unknown"` source file), and invokes the sink. Existing overloads pass `items -> {}`.

- [ ] **Step 7: Run tests to verify they pass**

Run: `mvn -q -pl renovatio-provider-cobol test -o -Dtest=CobolSemanticTranspilerTest,AnnotationActionItemFactoryTest`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/translation/ \
        renovatio-provider-cobol/src/test/java/org/shark/renovatio/provider/cobol/translation/
git commit -m "feat(cobol): map dropped annotations to manual action items in transpiler"
```

---

## Task 9: Wire `JavaGenerationService` to resolve the sidecar and write action items

**Files:**
- Modify: `renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/service/JavaGenerationService.java` (constructor + line ~87)
- Test: `renovatio-provider-cobol/src/test/java/org/shark/renovatio/provider/cobol/service/JavaGenerationServiceAnnotatedTest.java`

**Interfaces:**
- Consumes: `AnnotatedContextResolver`, `CobolSemanticTranspiler` three-arg overload, `ManualActionItemWriter`, `AnnotatedContextResolver.Request` (built from the migration `metadata` map — `filePath` gives `cobolSourcePath`; there is no inline sidecar on the current request, so `inlineSidecar`/`sidecarPath` are `Optional.empty()` until the MCP request type carries them).
- Produces: generated `ServiceImpl` reflects eligible renames/markers; a `manual-action-items.json` report is written under the workspace when any annotation is dropped; report path returned in `StubResult` metadata (or logged) — match existing reporting conventions.

- [ ] **Step 1: Write the failing test**

```java
package org.shark.renovatio.provider.cobol.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JavaGenerationServiceAnnotatedTest {

    @Test
    void generatedImplementationUsesCommittedSidecarRename(@TempDir Path workspaceDir) throws Exception {
        // Arrange a workspace with a .cob and a sibling <program>.annotated.json holding an
        // ACCEPTED DOMAIN_NAMING annotation. Reuse existing JavaGenerationServiceTest scaffolding
        // (parsingService stub, Workspace, NqlQuery) and just add the sidecar file.
        // Assert the returned ServiceImpl source contains the renamed identifier.
    }
}
```

> Flesh out the arrange block by copying the setup from the existing `JavaGenerationServiceTest` / `CalculatorGenerationTest`. Keep the assertion concrete: the renamed getter appears in `result.getGeneratedCode().get(classBase + "ServiceImpl.java")`.

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -q -pl renovatio-provider-cobol test -o -Dtest=JavaGenerationServiceAnnotatedTest`
Expected: FAIL.

- [ ] **Step 3: Implement the wiring**

- Add `AnnotatedContextResolver` and `ManualActionItemWriter` (constructed with the service's `ObjectMapper`, or inject) as fields; update the constructor and every caller / Spring config.
- At line ~87, replace:

```java
serviceImpl = semanticTranspiler.enrichServiceImplementation(serviceImpl, model);
```

with:

```java
Path cobolPath = Path.of((String) metadata.get("filePath"));
var resolution = annotatedContextResolver.resolve(
        new AnnotatedContextResolver.Request(Optional.empty(), Optional.empty(), cobolPath), model);
List<ManualActionItem> items = new ArrayList<>();
if (resolution.context().isPresent()) {
    serviceImpl = semanticTranspiler.enrichServiceImplementation(
            serviceImpl, resolution.context().get(), items::addAll);
} else {
    serviceImpl = semanticTranspiler.enrichServiceImplementation(serviceImpl, model);
}
if (!items.isEmpty()) {
    manualActionItemWriter.write(
        workspace.getPath().resolve("build/reports/renovatio/manual-action-items.json"), items);
}
```

Adjust `workspace.getPath()` to the actual Workspace API.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -pl renovatio-provider-cobol test -o -Dtest=JavaGenerationServiceAnnotatedTest,JavaGenerationServiceTest,CalculatorGenerationTest,ArithmeticMigrationTest`
Expected: PASS. Fix any Spring wiring test that constructs `JavaGenerationService` directly.

- [ ] **Step 5: Commit**

```bash
git add renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/service/JavaGenerationService.java \
        renovatio-provider-cobol/src/test/java/org/shark/renovatio/provider/cobol/service/JavaGenerationServiceAnnotatedTest.java
git commit -m "feat(cobol): resolve annotated sidecar and emit action items during generation"
```

---

## Task 10: Annotated characterization fixtures wired into the offline lane

**Files:**
- Modify: `renovatio-provider-cobol/src/test/java/org/shark/renovatio/provider/cobol/characterization/CharacterizationFixtureContractTest.java`
- Create: `renovatio-provider-cobol/src/test/resources/characterization/move-numeric/move-numeric.annotated.json`
- Create: `renovatio-provider-cobol/src/test/resources/characterization/move-numeric/expected-annotated.java`
- Create fixture dir: `renovatio-provider-cobol/src/test/resources/characterization/data-intent-redefines/` (`input.cob`, `expected-ir.json`, `expected-behavior.json`, `expected-action-items.json`, `translation-input.java`, `data-intent-redefines.annotated.json`, `expected-annotated.java`)
- Modify: `CharacterizationFixtureContractTest.FIXTURES` / `SUPPORTED` lists

**Interfaces:**
- Consumes: the `translate(...)` helper added in the #122 rework (base-model path), plus a new `translateAnnotated(Path cobol, Path javaStub, Path sidecar)` that goes through `AnnotatedContextResolver` + `CobolSemanticTranspiler` three-arg overload.
- Produces: for any supported fixture that has a `<fixtureId>.annotated.json`, the test runs `translateAnnotated` twice and asserts both are byte-identical to each other and to `expected-annotated.java`.

- [ ] **Step 1: Write the `move-numeric.annotated.json` sidecar**

Compute `baseIrHash` for the fixture's IR with `new CobolIrIdentityProjector().baseIrHash(model)` (add a throwaway `@Disabled` print test or a small `main`, capture the value, then delete). One `ACCEPTED` `DOMAIN_NAMING` annotation renaming `TARGET-NUM` → `roundedTotal` (or a name that does not collide in the generated `NumericDto`). Fill `annotationId`/`nodeId` with the real projector values.

- [ ] **Step 2: Write `expected-annotated.java`**

The `move-numeric` deterministic golden (from the #122 rework) with `targetNum` → `roundedTotal` everywhere (field, getter, setter, the `process` assignment, and the `CharacterizationFixture.run()` call site). It must still compile and, when executed, still return `-12.34` (rename does not change behavior — the `expected-behavior.json` observation is unchanged).

- [ ] **Step 3: Write the failing test branch**

In `CharacterizationFixtureContractTest`, inside the `SUPPORTED` block, after the existing base-path assertions:

```java
Path annotatedSidecar = fixture.resolve(fixtureId + ".annotated.json");
if (Files.exists(annotatedSidecar)) {
    String a1 = translateAnnotated(cobol, fixture.resolve("translation-input.java"), annotatedSidecar);
    String a2 = translateAnnotated(cobol, fixture.resolve("translation-input.java"), annotatedSidecar);
    assertThat(a1).as("independent annotated translations for %s", fixtureId).isEqualTo(a2);
    assertThat(a1).as("committed annotated golden for %s", fixtureId)
            .isEqualTo(Files.readString(fixture.resolve("expected-annotated.java")));
    Path out = compilationOutput.resolve(fixtureId + "-annotated");
    assertCompiles(a1, out);
    assertBehavior(out, behavior);
}
```

And the helper:

```java
private String translateAnnotated(Path cobol, Path javaStub, Path sidecar) throws Exception {
    CobolIntermediateModel model = modelService.parse(Files.readString(cobol));
    var resolution = new AnnotatedContextResolver(mapper).resolve(
            new AnnotatedContextResolver.Request(Optional.empty(), Optional.of(sidecar), cobol), model);
    assertThat(resolution.context()).as("sidecar %s must be valid", sidecar).isPresent();
    java.util.List<org.shark.renovatio.provider.cobol.guardrail.ManualActionItem> ignored = new java.util.ArrayList<>();
    return transpiler.enrichServiceImplementation(
            Files.readString(javaStub), resolution.context().get(), ignored::addAll);
}
```

- [ ] **Step 4: Run to verify it fails, then adjust goldens**

Run: `mvn -q -pl renovatio-provider-cobol test -o -Dtest=CharacterizationFixtureContractTest`
Expected: FAIL first (golden mismatch — copy the actual generated output into `expected-annotated.java` once it is verified correct by reading it), then PASS.

- [ ] **Step 5: Add the `data-intent-redefines` fixture**

`input.cob` with a `REDEFINES` clause; `translation-input.java` the DTO stub; `<id>.annotated.json` with one `ACCEPTED` `DATA_INTENT` annotation on the redefining data item; `expected-annotated.java` = deterministic golden + `@CobolDataIntent(...)` on the field; `expected-action-items.json` `{"schemaVersion":"manual-action-item.v1","items":[]}` (no drops); `expected-behavior.json` with the observed value. Add `"data-intent-redefines"` to `FIXTURES` and `SUPPORTED`.

- [ ] **Step 6: Run the full characterization + provider suite**

Run: `mvn -q -pl renovatio-provider-cobol test -o -Djacoco.skip=true`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add renovatio-provider-cobol/src/test/resources/characterization/ \
        renovatio-provider-cobol/src/test/java/org/shark/renovatio/provider/cobol/characterization/CharacterizationFixtureContractTest.java
git commit -m "test(cobol): annotated characterization fixtures for reproducible offline pass"
```

---

## Task 11: Extend the recipe-boundary purity test and Maven Enforcer

**Files:**
- Modify: the architecture/boundary test added for #123 (search: `grep -rl "src/main" cobol-openrewrite-recipes/src/test` — it scans production entries for provider/network/prompt references). Confirm exact class name.
- Modify: `cobol-openrewrite-recipes/pom.xml` Enforcer `bannedDependencies` (or wherever #123 added it) to also forbid provider/HTTP artifacts transitively via the new module.
- Test: extend the existing boundary test; add `renovatio-cobol-annotations` boundary assertion.

**Interfaces:**
- Consumes: existing boundary test infrastructure.
- Produces: assertions that `org.shark.renovatio.cobol.recipes.annotate.*` and `renovatio-cobol-annotations` classes import nothing from `org.shark.renovatio.provider`, `java.net.http`, `okhttp`, `org.apache.hc`, or any `*.llm.*` / `*.prompt.*` package.

- [ ] **Step 1: Locate the #123 boundary test**

Run: `grep -rn "runtime boundary\|no provider\|bannedImports\|src/main" cobol-openrewrite-recipes/src/test`
Read the class it points to.

- [ ] **Step 2: Write the failing assertion**

Add a test method asserting the `annotate` package and the annotations module are provider-free. Since no violation exists yet, temporarily add a bogus `import org.shark.renovatio.provider.cobol.guardrail.ManualActionItem;` to `AnnotationApplicator`, run, confirm the test FAILS, then remove the import.

- [ ] **Step 3: Run to verify the guard works**

Run: `mvn -q -pl cobol-openrewrite-recipes test -o -Dtest=<BoundaryTestClass>`
Expected: PASS after removing the bogus import; FAIL with it present.

- [ ] **Step 4: Extend the Enforcer rule**

Add `renovatio-cobol-annotations` to the allowed set and keep the banned dependency list (no `renovatio-provider-*`, no HTTP clients) enforced for the recipes module.

- [ ] **Step 5: Run the module build with Enforcer**

Run: `mvn -q -pl cobol-openrewrite-recipes -am verify -o -DskipTests=false -Djacoco.skip=true`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add cobol-openrewrite-recipes/pom.xml cobol-openrewrite-recipes/src/test
git commit -m "test(cobol): extend recipe purity boundary to annotation applicator"
```

---

## Task 12: Full verification, test report, and Agora artifacts

**Files:**
- Create: `docs/reports/annotated-openrewrite-pass-test-report-20260831.md`
- No code changes.

- [ ] **Step 1: Run the full COBOL reactor offline**

Run:
```bash
mvn -q -pl renovatio-cobol-annotations,cobol-openrewrite-recipes,renovatio-cobol-ir,renovatio-cobol-runtime,renovatio-provider-cobol -am test -o -Djacoco.skip=true
```
Expected: BUILD SUCCESS. Record per-module `Tests run` totals.

- [ ] **Step 2: Run the offline characterization lane exactly as CI does**

Run the same command the `Characterization guardrails (offline)` workflow runs (see `.github/workflows/`). Expected: green, no network.

- [ ] **Step 3: Write the test report**

`docs/reports/annotated-openrewrite-pass-test-report-20260831.md` — sections: scope, commands run, per-module results, the five acceptance criteria each mapped to the tests that prove it, offline confirmation, and any follow-ups.

- [ ] **Step 4: Commit the report**

```bash
git add docs/reports/annotated-openrewrite-pass-test-report-20260831.md
git commit -m "docs(agora): test report for annotated-openrewrite-pass (#127)"
```

- [ ] **Step 5: Register Agora artifacts and advance criteria**

```bash
agora artifact add --swarm ai-modernization --work annotated-openrewrite-pass --kind implementation-plan \
  --uri "repo://docs/plans/annotated-openrewrite-pass.md" --by project:agent
agora artifact add --swarm ai-modernization --work annotated-openrewrite-pass --kind test-report \
  --uri "repo://docs/reports/annotated-openrewrite-pass-test-report-20260831.md" --by project:agent
agora evidence add --swarm ai-modernization --work annotated-openrewrite-pass \
  --type unit-tests --result success \
  --artifact "repo://docs/reports/annotated-openrewrite-pass-test-report-20260831.md" --by project:agent
```
(Confirm `agora evidence` subcommand syntax with `agora evidence --help`.)

- [ ] **Step 6: Drive the lifecycle**

```bash
agora work transition --swarm ai-modernization --work annotated-openrewrite-pass --to planned --by project:agent
agora work transition --swarm ai-modernization --work annotated-openrewrite-pass --to implementing --by project:agent
# after evidence: verifying, then criteria -> accepted, spec-owner approval, completed
```

- [ ] **Step 7: Open the PR**

```bash
git push -u origin agora/issue-127-annotated-openrewrite-pass
gh pr create --title "feat(cobol): deterministic OpenRewrite pass over annotated IR (#127)" \
  --body "Implements docs/specs/annotated-openrewrite-pass.md. Closes #127."
```

Then run `/code-review` on the PR, address findings, merge, and close the Agora work item with spec-owner approval.

---

## Self-Review

**Spec coverage:**
- §3 applied families (DOMAIN_NAMING, DATA_INTENT) → Tasks 4, 5. Non-applied families → drop path Task 3 + mapping Task 8.
- §3.1 eligibility → Task 3 `eligible()`.
- §3.2 DOMAIN_NAMING rename + collision → Task 5.
- §3.3 DATA_INTENT marker → Task 4.
- §4 `@CobolDataIntent` module → Task 1.
- §5 context resolution precedence → Task 7; recipe post-pass → Task 6; transpiler drain + writer → Tasks 8, 9.
- §6 action-item mapping table → Task 8 factory.
- §7 purity + determinism → Task 11 (purity), Tasks 4/5/10 (determinism double-run).
- §8 acceptance scenarios → 8.1 Tasks 6/7, 8.2 Tasks 4/5/11, 8.3 Task 11, 8.4 Task 10, 8.5 Tasks 3/8/10.
- §9 construct-to-test matrix → Tasks 3–11 cover every row.
- §10 delivery artifacts → Task 12.

**Placeholder scan:** Task 9 Step 1 and Task 11 leave arrange-blocks / class names to be filled from existing tests — these are "copy this concrete existing scaffold" instructions, not open design. Everything with a code step has real code. `provenance()` helper in Task 3 references "copy the shape from an existing annotated-ir fixture test" — acceptable since that constructor is verbose and already exercised in `renovatio-cobol-ir` tests.

**Type consistency:** `DroppedAnnotation` fields/enum consistent across Tasks 3, 6, 8. `AnnotationApplicator` constructor `(CobolIntermediateModel, AnnotatedCobolModel)` consistent Tasks 3–6. `enrichServiceImplementation` three-arg `(String, AnnotatedCobolContext, Consumer<List<ManualActionItem>>)` consistent Tasks 8, 10. `AnnotatedContextResolver.Request(Optional<AnnotatedCobolModel>, Optional<Path>, Path)` consistent Tasks 7, 9, 10. `ANNOTATION_OUTCOMES_KEY` string `"renovatio.cobol.annotation-outcomes"` consistent Tasks 6, 8.

**Known risk to resolve during execution:** `JavaTemplate` needs `renovatio-cobol-annotations` on the OpenRewrite Java parser classpath (Task 4 Step 4 note). If it cannot be wired cleanly in the recipes-module test JVM, fall back to building the `@CobolDataIntent` annotation node with `JavaTemplate` using a fully-qualified name and `maybeAddImport`, or as a last resort a typeless annotation via `J.Annotation` construction — still AST, never string replacement.
