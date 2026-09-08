# Verificación #207 — `PERFORM` THRU / VARYING / UNTIL / TIMES y extracción de párrafos

Fecha: 2026-09-08

## Resultado final — revisión 0003

Los **10/10 criterios de aceptación** están satisfechos. La revisión 0003 cierra el último criterio,
`carddemo-compiles`, sin ampliar el contrato funcional del traductor:

- `spring-web` se incorpora con scope `test` al provider para que el compilador del harness pueda
  resolver `org.springframework.http.ResponseEntity` en controladores CICS generados.
- `CardDemoCoverageReportTest` incluye `VARYING` en el inventario léxico y falla si ningún programa
  CardDemo que contenga ese token produce Java compilable.
- El reporte regenerado mide **44/44 parse · 44/44 emit · 17/44 compila**.
- Compilan tres programas con `PERFORM VARYING`: `COADM01C`, `COCRDLIC` y `COTRTLIC`.

Esto mejora la compilación global desde 9/44 a 17/44 y convierte el criterio antes manual en una
regresión automatizada.

## Verificación de regresión

Comando:

```text
mvn -o -pl renovatio-cobol-ir,cobol-openrewrite-recipes,renovatio-provider-cobol test -Dexec.skip=true -Djacoco.skip=true
```

Resultado:

- `renovatio-cobol-ir`: 84 tests verdes.
- `cobol-openrewrite-recipes`: 34 tests verdes.
- `renovatio-provider-cobol`: 117 tests verdes.
- Total: **235 tests, 0 failures, 0 errors, 0 skipped**.

## Verificación CardDemo

Comando:

```text
mvn -o -pl renovatio-provider-cobol test -Dgroups=coverage -Drenovatio.surefire.excludedGroups= -Dexec.skip=true -Djacoco.skip=true
```

Resultado:

- `CardDemoCoverageReportTest`: 1 test verde.
- Corpus: 44 programas COBOL y 62 copybooks.
- Parse: 44; emisión Java: 44; compilación: 17.
- Gate `VARYING`: satisfecho por `COADM01C`, `COCRDLIC` y `COTRTLIC`.

## Cobertura de los criterios

| Criterio | Resultado | Evidencia principal |
| --- | --- | --- |
| `ir-representation` | ✅ | `PerformStatementParsingTest`; schema `cobol-ir.v1` |
| `method-extraction` | ✅ | `PopulateCobolProcessRecipeTest`; fixture `perform-simple-nested` |
| `perform-thru` | ✅ | fixture `perform-thru` |
| `looping-variants` | ✅ | TIMES, UNTIL BEFORE/AFTER y VARYING/AFTER en parser, recipe y fixtures |
| `recursion-guard` | ✅ | ciclo a `ManualActionItem`, sin recursión infinita |
| `traceability` | ✅ | `@GeneratedFrom` en métodos extraídos |
| `characterization` | ✅ | siete fixtures `perform-*` soportados |
| `nested-green` | ✅ | comportamiento `MAIN → OUTER-PARA → INNER-PARA` |
| `carddemo-compiles` | ✅ | gate automatizado y reporte 44/44/17 |
| `regression-green` | ✅ | 235 tests verdes y `git diff --check` limpio |

## Notas de alcance

Persisten fallos de compilación en otros programas por extracción incompleta de DTOs (`COMP-3`
multilínea, `BINARY`, nombres `FILLER` duplicados) y por construcciones COBOL todavía no modeladas.
No afectan el criterio #207, que exige al menos un programa CardDemo con `PERFORM VARYING`
compilable. El golden master E2E integral continúa bajo #216.
