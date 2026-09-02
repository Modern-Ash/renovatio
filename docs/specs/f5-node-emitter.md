# F5 NodeEmitter + Idiom Pattern Catalog

- **Work item:** `decision-engine-f5/f5-node-emitter`
- **GitHub issue:** #150 (Epic #152)
- **Method:** Agora `spec-driven` with TDD
- **Status:** governed specification
- **Date:** 2026-09-01
- **F4 compatibility baseline:** `69ae7fce`

## 1. Outcome

F5 validates the multi-target architecture by adding a Node.js/TypeScript emitter
as the second target language. A `NodeEmitter implements TargetEmitter` consumes
the same `TargetModel` produced by F3 architecture, generates TypeScript source
files, and is registered in `TargetEmitterRegistry` alongside `JavaEmitter`.
An idiom pattern catalog maps COBOL semantic intents to TypeScript idioms. A
Prisma persistence strategy is added for `NODE` target. The UI enables `NODE`
in the Target step language selector.

For the default Java profile, F5 is a no-op: existing Java output is unchanged.
The new emitter has no dependency on COBOL modules.

## 2. Scope and non-goals

F5 delivers:

1. new `renovatio-emitter-node` Maven module;
2. `NodeEmitter implements TargetEmitter` (supports `Language.NODE`);
3. `NodeArtifactRenderer` functional interface;
4. TypeScript code generation from `TargetModel` (service, entity, repository, config);
5. `NodeArchitectureLayoutPlanner` producing `.ts` artifact paths;
6. `PrismaStrategy implements PersistenceStrategy` for NODE target;
7. `NodeIdiomCatalog` with basic semantic-intent-to-TypeScript mappings;
8. `RenovatioPrismaSchemaGenerator` emitting `schema.prisma` file;
9. UI: NODE enabled in Target step language dropdown; and
10. API: `GET /api/projects/{id}/node-preview` returning generated TS source.

F5 does not deliver:

- Python emitter (later phase);
- multiple Node frameworks (Express only in F5);
- full idiom parity (missing patterns → manual action items);
- real database migration or DDL generation;
- runtime verification of generated Node projects;
- changes to COBOL IR, semantic IR, or Java provider; or
- `npm install` / build execution in the generated project.

## 3. Module and dependency boundary

```text
renovatio-semantic-ir   renovatio-profile
          \                /
        renovatio-shared (emission envelope)
                    |
        renovatio-architecture (layout planner SPI)
                    |
        renovatio-emitter-node (NEW)
                    |
       provider/core orchestration -> TargetEmitterRegistry
```

`renovatio-emitter-node` depends on: `renovatio-shared`, `renovatio-architecture`.
It must NOT depend on: `renovatio-provider-java`, `renovatio-provider-cobol`,
`renovatio-cobol-ir`, `renovatio-cobol-annotations`, OpenRewrite, JavaPoet,
Spring MVC, JPA, or any COBOL module.

## 4. NodeEmitter contract

```java
public final class NodeEmitter implements TargetEmitter {
    public boolean supports(MigrationProfile.Language target) {
        return target == MigrationProfile.Language.NODE;
    }
    public EmittedArtifacts emit(TargetModel model, MigrationProfile profile) {
        // validates profile == model.profile()
        // delegates to NodeArtifactRenderer
    }
}
```

`NodeArtifactRenderer` is a `@FunctionalInterface`:

```java
@FunctionalInterface
public interface NodeArtifactRenderer {
    EmittedArtifacts render(TargetModel model, MigrationProfile profile);
}
```

Generated artifacts per program:

| Component | Path pattern | Content |
|---|---|---|
| Service (use case) | `src/domain/{module}/{ProgramId}.service.ts` | TypeScript class with methods from structured paragraphs |
| Entity (model) | `src/domain/{module}/{ProgramId}.entity.ts` | TypeScript interface/class from semantic types |
| Repository (port) | `src/domain/{module}/{ProgramId}.repository.ts` | Interface abstracting data access |
| Controller (inbound) | `src/api/{module}/{ProgramId}.controller.ts` | Express route handler |
| App bootstrap | `src/main.ts` | Express app setup |
| Package manifest | `package.json` | Dependencies, scripts |
| TypeScript config | `tsconfig.json` | Compiler options |
| Prisma schema | `prisma/schema.prisma` | From PrismaStrategy |

## 5. NodeArchitectureLayoutPlanner

Implements `ArtifactLayoutPlanner` for `Language.NODE`:

- `TRANSACTION_SCRIPT`: one service + entity + repository + controller per program
- `HEXAGONAL`: services in `src/domain/`, controllers in `src/api/`, adapters in `src/infrastructure/`
- Module grouping from F3 profile: `BY_PROGRAM` = one module dir per program, `BY_DOMAIN` = grouped by domain

## 6. PrismaStrategy

```java
public final class PrismaStrategy implements PersistenceStrategy {
    public boolean supports(DataAccessClassification classification, MigrationProfile.Language target) {
        return target == MigrationProfile.Language.NODE
                && classification.kind() != DataAccessKind.RESIDUAL;
    }
    public PersistenceArtifacts emit(DataAccessClassification classification,
                                     MigrationProfiles.EffectiveProfile profile) {
        // generates TypeScript entity + Prisma repository + schema.prisma snippet
    }
}
```

Generated artifacts:
- `Prisma{Entity}.ts` — Prisma client wrapper
- `schema.prisma` snippet — model definition with fields from record shape
- Prisma strategy also generates `prisma/seed.ts` with stub data

## 7. Idiom Pattern Catalog

`NodeIdiomCatalog` maps COBOL semantic intents to TypeScript equivalents:

| COBOL construct | TypeScript idiom |
|---|---|
| MOVE | assignment |
| COMPUTE | expression |
| IF/EVALUATE | if/switch |
| PERFORM | async function call |
| READ FILE | `await fs.readFile()` or Prisma `findMany()` |
| WRITE FILE | `await fs.writeFile()` or Prisma `create()` |
| EXEC SQL | Prisma client method |
| DISPLAY | `console.log()` or Express response |
| ACCEPT | Express request.body / params |

Missing patterns produce manual action items, not guessed code.

## 8. Verification

1. Same multi-program COBOL fixture migrated to Java (F3) and Node (F5) →
   both have equivalent use case / port structure;
2. Generated Node project for a small fixture responds to HTTP requests;
3. No changes to `renovatio-cobol-ir`, `renovatio-semantic-ir`, or
   `renovatio-provider-cobol` in F5 diff (except documented leak fixes);
4. `mvn clean install` green; ArchUnit proves no forbidden dependencies;
5. Default Java profile produces byte-identical output to F4 baseline;
6. `TargetEmitterRegistry` resolves both `JAVA` → `JavaEmitter` and
   `NODE` → `NodeEmitter`.

## 9. Acceptance mapping

| Agora criterion | Normative sections |
|---|---|
| `node-emitter` | §§4, 7 define the emitter contract and code generation |
| `layout-planner` | §5 defines the artifact layout for Node |
| `prisma-strategy` | §6 defines the Prisma persistence strategy |
| `idiom-catalog` | §7 defines the pattern catalog |
| `ui-enablement` | §10 UI enablement (Target step) |
| `compatibility` | §§2, 8 define scope boundaries and Java baseline preservation |
| `verification-scope` | §8 defines required evidence gates |
