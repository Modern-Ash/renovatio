# Plan de implementación · Reporte de cobertura CardDemo (#217)

- **Spec:** `docs/specs/issue-217-carddemo-coverage.md`
- **Rama:** `agora/issue-218-manifest-consistency` (worktree `/tmp/renovatio-215`, base `main`)
  — se renombrará a `agora/issue-217-carddemo-coverage` antes del primer commit de código.

## Fase 1 — Vendorizar el corpus (R1)

Copiar desde `demo/aws-mainframe-modernization-carddemo/` (hoy untracked) el subconjunto de
fuentes a `renovatio-provider-cobol/src/test/resources/corpus/carddemo/`:

```
corpus/carddemo/
  LICENSE                 (Apache-2.0)
  NOTICE
  PROVENANCE.md           (repo origen, URL, commit/fecha de captura, licencia, subset incluido)
  app/cbl/                *.cbl *.CBL   (31)
  app/cpy/                *.cpy
  app/cpy-bms/            *.cpy
  app/jcl/                *.jcl *.JCL   (85)
  app/app-transaction-type-db2/{cbl,cpy,cpy-bms,jcl}/
  app/app-authorization-ims-db2-mq/{cbl,cpy,cpy-bms,jcl}/
  app/app-vsam-mq/cbl/
```

- Excluir: `diagrams/`, `samples/`, `scripts/`, `data/`, `asm/`, `maclib/`, `catlg/`, `csd/`,
  `bms/` (fuente ASM de mapas), imágenes, `.md` de gobernanza del repo origen.
- `.gitattributes`: marcar el corpus como `linguist-vendored` y `-text` (no normalizar EOL).
- Añadir `corpus/**` a cualquier exclusión de formato/checkstyle si aplica a `src/test/resources`
  (normalmente no aplica a resources, verificar).
- ~2.3 MB, sólo texto.

## Fase 2 — Herramienta de cobertura (R2)

Nueva clase de test: `renovatio-provider-cobol/src/test/java/.../coverage/CardDemoCoverageReportTest.java`
con `@Tag("coverage")`.

- Localiza el corpus vía classpath/`src/test/resources/corpus/carddemo` (path relativo al módulo,
  sin absolutos).
- Descubre programas con `CobolParsingService.findCobolSourceFiles(...)` + filtro D1.
- Por programa, en un `@TempDir` con el `.cbl` + copybooks del mismo subsistema copiados:
  1. `parsingService.analyzeCOBOL(...)` → `parse.ok`, error.
  2. `CobolIntermediateModelService` / `CobolSemanticProjector` → `ir.ok`, dropped constructs.
  3. `JavaGenerationService(...).generateInterfaceStubs(query, workspace)` (perfil por defecto)
     → `emit.ok`, nº archivos, flags `hasTodo` / `hasUnhandled`, lista de `ManualActionItem`.
  4. Compilar el Java emitido con `ToolProvider.getSystemJavaCompiler()` en un classpath que
     incluya `spring-context` (por `@Service`) → `compile.ok`, primeros diagnósticos.
  5. Clasificar subsistema (D2); contar verbos presentes (grep) vs no soportados (D3).
- Acumular en una estructura `CoverageRow` por programa + agregados.
- Si `JavaGenerationService` no expone los `ManualActionItem` de una corrida in-memory, añadir un
  accesor aditivo (D5) — preferible a releer el `manual-actions.json` del workspace, pero
  cualquiera de las dos vale.
- Quitar los `System.out.println("DEBUG…")` de `JavaGenerationService` / `generateServiceImplementation`
  (limpieza permitida por D5; hoy ensucian la salida de todos los tests).

## Fase 3 — Emisión de reportes (R3, R4, R5)

El test escribe dos ficheros al repo (ruta `docs/reports/` resuelta desde el módulo con
`../docs/reports/`):

- `carddemo-coverage.json` — determinista: sin timestamps, paths relativos al corpus, listas
  ordenadas por `PROGRAM-ID`, números enteros. Esquema:
  ```json
  {
    "corpus": "carddemo",
    "totals": {"programs": N, "parsed": _, "emitted": _, "compiled": _},
    "bySubsystem": {"batch": {...}, "cics-online": {...}, ...},
    "unsupportedConstructs": [{"construct": "DISPLAY", "programs": k, "occurrences": m}, ...],
    "e2eCandidates": ["CBACT02C", "CBACT03C", "CBCUS01C"],
    "programs": [{"programId": "...", "file": "...", "loc": _, "subsystem": "...",
                 "parse": true, "ir": true, "emit": true, "compile": false,
                 "javaFiles": _, "manualActionItems": _, "hasTodo": _, "hasUnhandled": _,
                 "unsupported": ["GOTO", "SEARCH"]}],
    "inventory": {"included": [...], "excludedSameProgramId": [...]}
  }
  ```
- `carddemo-coverage.md` — render legible del JSON: resumen, tabla por subsistema, top-15 de
  constructos no soportados, tabla por programa, y la sección "Candidatos para el E2E #216".

El test **no** falla por cobertura baja; sólo falla si no puede producir los reportes o si el
corpus está vacío (R6). Un assert mínimo: `totals.programs >= 40` y los ficheros se escribieron.

## Fase 4 — Reproducibilidad y CI (R6)

- Surefire: excluir `@Tag("coverage")` del `test` por defecto
  (`<excludedGroups>coverage</excludedGroups>` en el `pom` del módulo o vía property).
- Perfil Maven `-Pcoverage-report` (o `mvn test -Dgroups=coverage -pl renovatio-provider-cobol`)
  documentado en `README` / `docs/reports/README.md`.
- Job CI nightly (o step manual) — describir en el plan de CI existente; no bloquear PRs.
- Verificar que corre offline: los copybooks necesarios se copian al `@TempDir`; no hay red.

## Fase 5 — Verification report

`docs/reports/issue-217-carddemo-coverage-verification.md`: comando exacto, resumen de números
obtenidos, confirmación de R7 (diff sin tocar traductor, `renovatio-provider-cobol` 114/114 +
suites de architecture/provider-java), y los 3 candidatos E2E con su justificación de orden (D4).

## Riesgos

- **Copybook resolution:** muchos programas `COPY` de `app/cpy`. Si el parser no resuelve
  copybooks fuera del cwd, copiar todo `app/cpy` + `cpy-bms` del subsistema al `@TempDir`.
- **Compilación:** el Java emitido referencia `org.springframework.stereotype.Service`; incluir
  esa dependencia en el classpath del compilador (ya es dependencia del módulo).
- **Tiempo:** ~44 programas × (parse+emit+compile). Estimado < 3 min; si excede, permitir
  `-Dcarddemo.coverage.limit=N` para corridas parciales (no afecta el reporte committeado).
- **Determinismo del JSON:** cuidar orden de mapas (usar `TreeMap`/listas ordenadas) y no incluir
  hashes que cambien entre entornos.

## Commits (Conventional Commits)

1. `test(corpus): vendor AWS CardDemo COBOL/JCL sources (Apache-2.0) (#217)`
2. `test(coverage): add CardDemo pipeline coverage report tool (#217)`
3. `chore(cobol): drop debug System.out from Java generation (#217)`
4. `docs(reports): publish first CardDemo coverage report (#217)`
5. `docs(agora): verification report for issue #217`
