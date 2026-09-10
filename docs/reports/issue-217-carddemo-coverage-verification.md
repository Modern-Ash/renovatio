# Verification · Reporte de cobertura CardDemo (#217)

- **Swarm/work:** `issue-217-carddemo-coverage` (043) / `carddemo-coverage-report`
- **Rama:** `agora/issue-217-carddemo-coverage` (base `main` `5835fdeb`)
- **Fecha:** 2026-09-08

## Comando

```bash
mvn -pl renovatio-provider-cobol test \
    -Dgroups=coverage -Drenovatio.surefire.excludedGroups= -Dexec.skip=true
```

`Tests run: 1, Failures: 0, Errors: 0 — BUILD SUCCESS`. Genera
`docs/reports/carddemo-coverage.{md,json}`.

## Resultados de la primera corrida

| Métrica | Valor |
| --- | --- |
| Programas COBOL en el corpus | 44 |
| Parsean | 44 / 44 |
| Emiten Java | 44 / 44 |
| El Java generado compila con `javac` | **10 / 44 (23 %)** |
| Copybooks | 62 |

Por subsistema (compilan / total): batch **9/14**, db2 **1/3**, cics-online **0/19**,
ims-mq **0/8**.

Top de constructos presentes que el traductor no cubre hoy (heurística léxica):
`SET` (36 programas), `DISPLAY` (34), `INITIALIZE` (32), `CONTINUE` (31), `ADD` (30),
`STRING` (24), `GOBACK` (19), `GO TO` (10), `SUBTRACT` (10), `SEARCH` (8).

`unhandledStatements` = 0 en todos los programas → confirma que el recipe descarta las sentencias
no soportadas en silencio en vez de dejar un marcador `// Unhandled COBOL statement` (dato para
#206). `manualActionItems` = 0 en todos → el path por defecto (sin sidecar anotado) no está
emitiendo action items para lo que descarta (dato para #215/#206).

### Candidatos E2E para #216 (D4 — batch, parse+emit OK, orden determinista)

1. `COBSWAIT` — 41 LOC, compila. Trivial (solo `CALL 'CEE3DLY'`); útil como smoke mínimo.
2. `CSUTLDTC` — 157 LOC, compila. Utilidad de conversión de fecha (`CALL 'CEEDAYS'`…); buen
   caso con lógica real y sin I/O de archivos.
3. `CBACT02C` — 178 LOC, compila. Lectura secuencial de un fichero de tarjetas; el primer caso
   con file I/O real (aunque el I/O hoy no se traduce — ver #213).

`CBACT01C` (430 LOC, lectura de cuentas) queda 4º: no compila todavía; es el objetivo natural una
vez cerrados algunos gaps.

## R7 — sin cambio de motor

`git diff main...HEAD --stat` sobre `src/main`:
- `renovatio-provider-cobol/pom.xml` — sólo añade config Surefire (`excludedGroups`) y la
  property `renovatio.surefire.excludedGroups`. Sin cambios de código.
- Ningún cambio en `PopulateCobolProcessRecipe`, `CobolSemanticTranspiler`, `JavaGenerationService`,
  plantillas ni mapeo de tipos.

Regresión:
- `mvn -o test -pl renovatio-provider-cobol -Dexec.skip=true` → **114 / 114**, y
  `CardDemoCoverageReportTest` **no** se ejecuta (excluido por tag `coverage`).
- `mvn -o test -pl renovatio-architecture,renovatio-provider-java -Dexec.skip=true` → BUILD SUCCESS.

## Criterios

| Criterio | Estado | Evidencia |
| --- | --- | --- |
| `corpus-vendored` | ✅ | `renovatio-provider-cobol/src/test/resources/corpus/carddemo/` (44 cbl, 62 cpy, 46 jcl) + `LICENSE`, `NOTICE`, `PROVENANCE.md`; `.gitattributes` `linguist-vendored` |
| `coverage-tool` | ✅ | `CardDemoCoverageReportTest` (`@Tag("coverage")`) parse→emit→javac por programa |
| `aggregate-report` | ✅ | `docs/reports/carddemo-coverage.md` (totales, subsistemas, constructos, tabla por programa) |
| `tracking-json` | ✅ | `docs/reports/carddemo-coverage.json` determinista (orden por PROGRAM-ID, sin timestamps/paths absolutos) |
| `e2e-candidates` | ✅ | sección "E2E candidates" + `e2eCandidates` en el JSON |
| `reproducible-ci` | ✅ | comando único documentado en `docs/reports/carddemo-coverage-README.md`; corre offline (copybooks al `@TempDir`); excluido del build por defecto |
| `no-engine-change` | ✅ | ver R7 arriba |
