# Verification · #206 ciclo 1 — DISPLAY / CONTINUE / GOBACK / STOP RUN + sin drops silenciosos

- **Swarm/work:** `issue-206-display-continue-goback` (044) / `display-continue-goback`
- **Rama:** `agora/issue-206-control-io-verbs` (base `main` `5835fdeb`)
- **Fecha:** 2026-09-08

## Cambios

| Módulo | Archivo | Qué |
| --- | --- | --- |
| `renovatio-cobol-ir` | `model/SimpleStatement.java` (nuevo) | record `SimpleStatement(Kind, text)`, `Kind ∈ {DISPLAY, CONTINUE, GOBACK, STOP_RUN, UNTRANSLATED}` |
| `renovatio-cobol-ir` | `parser/SimpleCobolIrParser.java` | `parseSimpleStatement`: reconoce los 4 verbos; toda otra línea no-ruido de PROCEDURE DIVISION → `UNTRANSLATED` (antes se descartaba). `DISPLAY`/`CONTINUE` añadidos a `RESERVED_PARAGRAPH_TOKENS` para que `X.` no se confunda con cabecera de párrafo |
| `cobol-openrewrite-recipes` | `PopulateCobolProcessRecipe.java` | `renderSimple`: DISPLAY→`System.out.println(...)`, CONTINUE→`; // CONTINUE`, GOBACK/STOP RUN→`return <out>;`, UNTRANSLATED→`// COBOL not translated: <≤120 chars>`. `terminatesFlow` corta el render tras GOBACK/STOP RUN (y tras un `return` de nivel superior llegado por PERFORM inline) para no dejar Java inalcanzable. `buildBody` ya no duplica el `return` final |
| `renovatio-cobol-ir` | `annotated/CobolIrIdentityProjector.java` | `SimpleStatement` no lleva anotaciones → excluido de la proyección de identidad (`identityBearing` filtra, `addStatements` lo salta) |
| tests | `SimpleStatementParsingTest` (nuevo, 7), `PopulateCobolProcessRecipeTest#shouldRenderDisplayContinueGobackAndSurfaceUntranslatedStatements` (nuevo) | |
| tests | `PopulateCobolProcessRecipeTest#shouldInlinePerformParagraphs` | fixture ajustada: `PREP-PARA` termina en `EXIT` en vez de `GOBACK` (GOBACK en un párrafo performado termina el run unit en COBOL real; el fixture usaba GOBACK sólo como terminador) |
| tools | `carddemo/CardDemoCoverageReportTest.java` | DISPLAY/CONTINUE/GOBACK/STOP RUN añadidos a `SUPPORTED_VERBS`; nueva columna `notTranslatedMarkers` |

## Regresión

CI Java real = `.github/workflows/characterization-offline.yml`:

```
mvn -B -o -pl renovatio-provider-cobol,cobol-openrewrite-recipes -am test -Dexec.skip=true
```

→ **BUILD SUCCESS**, reactor completo verde: `renovatio-cobol-ir` 62, `cobol-openrewrite-recipes`
25, `renovatio-provider-cobol` **114/114**. Además `renovatio-architecture` y
`renovatio-provider-java` verdes por separado.

**Nota sobre R7 / gate jacoco:** los tres módulos declaran `jacoco:check` con
`LINE COVEREDRATIO minimum 1.0` en la fase `verify`, pero **CI nunca ejecuta `verify`** (sólo
`test`), y el gate ya está roto en `main` (`renovatio-cobol-ir` = 0.60 de cobertura de línea
antes de este cambio). Este ciclo añade tests para todo su código nuevo (parser + recipe cubiertos
por `SimpleStatementParsingTest` y el test de recipe) pero **no** sube el módulo a 100 % — eso es
deuda preexistente fuera de alcance. El criterio `regression-green` se cumple contra el comando de
CI real.

## Delta de cobertura CardDemo (#217)

Re-generado `docs/reports/carddemo-coverage.{md,json}` con
`mvn -pl renovatio-provider-cobol test -Dgroups=coverage -Drenovatio.surefire.excludedGroups= -Dexec.skip=true`:

| | antes (#217) | después (#206 c1) |
| --- | --- | --- |
| Programas que compilan | 10 / 44 | **11 / 44** |
| `DISPLAY` en lista "sin traducir" | sí (34 prog) | **no** |
| `CONTINUE` | sí (31) | **no** |
| `GOBACK` | sí (19) | **no** |
| `STOP RUN` | sí (2) | **no** |
| Marcadores `// COBOL not translated:` | 0 (silencio) | **7223** en 10 programas |

`CBACT01C` y `CBCUS01C` pasan de no-compilar a compilar. Candidatos E2E #216 ahora:
`CBCUS01C`, `CBACT01C`, `COBSWAIT`.

## Criterios

| Criterio | Estado | Evidencia |
| --- | --- | --- |
| `display-rendered` | ✅ | `SimpleStatementParsingTest.parsesDisplayWith*`, `PopulateCobolProcessRecipeTest#shouldRender...` (`System.out.println("RATING " + input.getCustomerRating())`) |
| `continue-noop` | ✅ | mismo test recipe (`; // CONTINUE`) |
| `goback-return` | ✅ | mismo test recipe (`return output;` ×2, sin código tras el return de la rama) |
| `no-silent-drop` | ✅ | `// COBOL not translated: INITIALIZE CUSTOMER-NAME`; coverage report: 7223 marcadores, 0 `// Unhandled`; los 44 programas emiten y ninguno rompe javac *por sintaxis nueva* introducida acá |
| `coverage-delta` | ✅ | tabla arriba; compilan 11 ≥ 10; los 4 verbos fuera de la lista |
| `regression-green` | ✅ (con la nota jacoco) | comando CI BUILD SUCCESS; 114/114 provider-cobol |

## Fuera de alcance / decisiones

- **Fixture de caracterización de corpus:** no se añadió a
  `CharacterizationFixtureContractTest.FIXTURES` — requiere golden Java + observaciones de
  comportamiento ejecutables, desproporcionado para 4 verbos triviales. Los tests directos de
  parser + recipe dan cobertura equivalente y son el patrón ya usado en ese módulo.
- **`ManualActionItem` propagado al reporte de action-items:** el marcador `// COBOL not
  translated:` es visible en el código pero no se convierte (todavía) en un `ManualActionItem`
  del `manual-action-items.json`. Ciclo aparte (necesita el plumbing recipe→outcome).
- **DISPLAY multilínea:** cae a `UNTRANSLATED`.
