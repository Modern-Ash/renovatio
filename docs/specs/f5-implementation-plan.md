# F5 Implementation Plan

## Phase 1: Maven Module Setup
- Create `renovatio-emitter-node` POM with dependencies on `renovatio-shared`, `renovatio-architecture`, `renovatio-profile`, `renovatio-persistence`
- Create `module-info.java` exporting packages
- Register in root POM `modules` and `dependencyManagement`

## Phase 2: Core Emitter
- `NodeArtifactRenderer` functional interface
- `NodeEmitter implements TargetEmitter` (supports `Language.NODE`)
- `DefaultNodeRenderer` generating TypeScript artifacts

## Phase 3: Layout Planner
- `NodeArchitectureLayoutPlanner implements ArtifactLayoutPlanner` for NODE
- Maps component kinds to `.ts` file paths

## Phase 4: Prisma Strategy
- `PrismaStrategy implements PersistenceStrategy` for NODE target
- Generates Prisma schema and TypeScript repository

## Phase 5: Idiom Catalog
- `NodeIdiomCatalog` with 10 COBOL → TypeScript mappings

## Phase 6: REST Endpoint
- `NodePreviewController` with `GET /api/projects/{id}/node-preview`
- `NodePreviewService` generating preview files

## Verification
- `mvn test -pl renovatio-emitter-node -am` passes
- 6 tests: NodeEmitterTest, PrismaStrategyTest (2), NodeIdiomCatalogTest (3)
