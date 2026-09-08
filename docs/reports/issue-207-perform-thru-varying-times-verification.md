# Verificación #207 — `PERFORM` THRU / VARYING / UNTIL / TIMES y extracción de párrafos

Fecha: 2026-09-08

---

## Revisión 0002 — cierre de `looping-variants` y `characterization`

`PERFORM VARYING ... AFTER` (ejes de iteración secundarios) pasa de gap conocido a implementado:

- **IR:** `PerformStatement` gana el componente `List<VaryingAxis> varyingAfter`
  (`VaryingAxis(variable, from, by, until)`); constructor de conveniencia de 9 args preservado.
  `cobol-ir.v1.schema.json` declara `varyingAfter`. `CobolIrIdentityProjector` lo proyecta.
- **Parser:** `SimpleCobolIrParser.PERFORM_AFTER` extrae los ejes `AFTER` y los separa antes de
  parsear el `UNTIL` primario (que si no los absorbía por ser `.+$`). Cubre forma con párrafo,
  `THRU` e inline `END-PERFORM` (en una sola línea lógica).
- **Render:** `PopulateCobolProcessRecipe.varyingLoop` emite un `for` por eje; los `AFTER`
  anidan dentro del eje primario, el último `AFTER` es el bucle más interno. Alias de variable
  de control y `assigned`-set contemplan todos los ejes.
- **Fixtures nuevos** (`SUPPORTED`, con `expected.java` que compila y comportamiento verificado):
  - `perform-until-after` → `do { output.setWsCount(1); } while (!(output.getWsCount() > 0));`,
    observación `"1"`.
  - `perform-varying-after` → `for (wsI…) { for (wsJ…) { output.setWsTraceNum(wsJ); } }`,
    observación `"3"`.

### Regresión (revisión 0002)

- `renovatio-cobol-ir`: 84 tests verdes (+2: `parsesPerformVaryingWithAfterAxes`,
  `parsesInlineVaryingWithAfterAxis`).
- `cobol-openrewrite-recipes`: 34 tests verdes (+1: `shouldRenderPerformVaryingAfterAsNestedLoops`).
- `renovatio-provider-cobol`: verde, `CharacterizationFixtureContractTest` cubre 19 fixtures
  (2 nuevos).
- Cobertura CardDemo: **44/44/44/9** sin cambio (ningún programa del corpus con `VARYING`
  compila aún; causas preexistentes descritas abajo).
- `git diff --check` limpio.

### Criterios tras la revisión 0002

Satisfechos **9/10**: los 7 previos + `looping-variants` + `characterization`.
Pendiente **1/10**: `carddemo-compiles`, diferido formalmente a #216 (ver «Criterio
`carddemo-compiles`» más abajo — sin cambios respecto a la revisión 0001).

---

## Resultado

Todas las verificaciones de regresión finalizaron con código 0 (232 tests):

- `renovatio-cobol-ir`: 82 tests verdes.
- `cobol-openrewrite-recipes`: 33 tests verdes (incluye `PopulateCobolProcessRecipeTest`).
- `renovatio-provider-cobol`: 117 tests verdes (incluye `JavaGenerationServiceTest`,
  `CharacterizationFixtureContractTest`, `GuardrailSchemaCatalogTest`).
- Cobertura CardDemo: 44/44 parse, 44/44 emit, **9** programas con Java que compila
  (baseline del lote: 32/44 emit y 11/44 compila en HEAD); el crash de COPAUA0C ya no ocurre
  (regresión previa detectada: 44/21/8).
- `git diff --check` limpio.

## Entregables del ciclo

- Guarda de referencias a datos no modelados en `PopulateCobolProcessRecipe`: una referencia que
  no es un item del IR (ni variable asignada) degrada la sentencia completa a
  `// COBOL not translated: <name> (data item not modeled)`, manteniendo el LST válido y la
  generación estable (ManualActionItem estable, sin `StackOverflowError`).
  Con esta guarda desapareció el crash de COPAUA0C (un NPE al formatear un árbol con nodos
  inválidos) y la emisión pasó de 36 a 44 programas.
- Normalización de subíndices (`(1)`/`(2)`): la DTO aplana los campos OCCURS a escalares, así que
  setters/getters y resolución de referencias ignoran los subíndices (antes `setArrAcctCurrCycDebit(1)(100500)`
  rompía el `javac` con `';' expected`).
- CICS: en `CobolParsingService.extractCicsCommands` se descartan los números de secuencia de
  formato fijo (columnas 73-80) que se capturaban como "comandos" fantasma (`02980000`,
  `02750012`, `04530000`, `061700`) y que producían `public ResponseEntity<String> 02980000(...)`
  (error `<identifier> expected`). En `JavaGenerationService.javaMethodName` se escapan keywords
  (`EXEC CICS RETURN` → `return_`) y se antecede `tx` si el nombre no es un identificador Java
  válido.
- Correcciones de PERFORM (THRU, VARYING/UNTIL/TIMES, inline, extracción a métodos `performX`,
  trazabilidad `@GeneratedFrom`, detección de ciclos como action item) permanecen verdes desde la
  etapa anterior del ciclo.

## Criterios no satisfechos

Tres de los diez criterios de aceptación quedan sin satisfacer en esta revisión; se documentan
con precisión para la fase de verificación y para los follow-ups:

1. **`carddemo-compiles`** — ver detalle abajo. Causas preexistentes ajenas a PERFORM.
2. **`looping-variants`** — la cláusula `AFTER` de `PERFORM VARYING` (segundo eje / bucles
   anidados, p.ej. `PERFORM VARYING A ... AFTER B ...`) no se representa en el IR ni se renderiza:
   `SimpleCobolIrParser.PERFORM_VARYING` captura un solo eje (`varyingVariable/from/by/until`), y
   `wrapPerform` emite un único `for`. El resto de la variante (TIMES, UNTIL TEST BEFORE/AFTER,
   VARYING 1-eje, inline `END-PERFORM`) sí está cubierto.
3. **`characterization`** — los fixtures cubren `perform-simple-nested`, `perform-thru`,
   `perform-times`, `perform-until` (TEST BEFORE) y `perform-varying` (1 eje), pero faltan los
   fixtures con Java esperado para `UNTIL TEST AFTER` y `VARYING ... AFTER` (bucles anidados).

## Criterio `carddemo-compiles` (estado honesto)

La especificación pide "un programa del corpus CardDemo con `PERFORM VARYING` produce Java que
compila". Ningún programa del corpus con `VARYING` compila hoy; se documentan las causas,
todas preexistentes y ajenas a la lógica de PERFORM:

- Programas con `VARYING`: CBACT01C (batch), CBSTM03A (batch), COUSR00C, COTRN00C, COMEN01C,
  CORPT00C, COADM01C, COCRDLIC (CICS), COPAUS0C (ims-mq), COTRTLIC (db2).
- CICS (y COPAUS0C/COTRTLIC vía `CicsController`): bloqueados por la ausencia de `spring-web`
  en el `pom` del harness (`import org.springframework.http.ResponseEntity` no resuelve).
  Independiente de PERFORM; añadir la dependencia queda fuera del alcance de #207.
- CBACT01C (único `VARYING` batch): quedó a **4** errores de compilar, todos de extracción léxica
  de WORKING-STORAGE en la generación del DTO (`CobolParsingService.extractDataItems` /
  `WS_FIELD_PATTERN`), preexistentes y no relacionados con PERFORM:
  - `out.setArrAcctCurrCycDebit(100500)` x2 — el campo `ARR-ACCT-CURR-CYC-DEBIT` se declara
    `PIC S9(10)V99` con `USAGE IS COMP-3.` en la línea siguiente; el patrón exige el punto en la
    misma línea, así que el campo no entra al DTO. Además el literal entero 100500 no encaja con
    el tipo BigDecimal que requeriría.
  - `out.setTiming(0)` / `out.setAbcode(999)` — `PIC S9(9) BINARY.`: el patrón ignora la cláusula
    `BINARY`.
- CBSTM03A: DTO con campos `filler` duplicados (preexistente).

Decisión de alcance tomada en implementación: se implementó la guarda de no-modelados (los refs
que no existen como campo del DTO bajan a comentario en lugar de emitir `get/set` que no resuelve)
y se acepta/documented el delta. El criterio en su forma estricta queda pendiente de dos gaps
preexistentes ajenos a PERFORM: la robustez de la extracción léxica del DTO y el classpath CICS.
Follow-ups recomendados: (1) generalizar `WS_FIELD_PATTERN` a `BINARY`, usuario en línea siguiente
(`USAGE IS COMP-3.`) y valor decimale, alineando el DTO con el IR; (2) `spring-web` en el harness.

## Comandos

```text
mvn -o -pl renovatio-cobol-ir,cobol-openrewrite-recipes,renovatio-provider-cobol test -Dexec.skip=true -Djacoco.skip=true

mvn -o -pl cobol-openrewrite-recipes install -DskipTests -Dexec.skip=true -Djacoco.skip=true
mvn -o -pl cobol-openrewrite-recipes test -Dexec.skip=true -Djacoco.skip=true

mvn -o -pl renovatio-provider-cobol test -Dexec.skip=true -Djacoco.skip=true

mvn -o -pl renovatio-provider-cobol test -Dgroups=coverage -Drenovatio.surefire.excludedGroups= -Dexec.skip=true -Djacoco.skip=true

git diff --check
```

Evidencia incluida: `docs/reports/carddemo-coverage.md` (+ `.json`) regenerados con 44/44/44/9.

Criterios declarados satisfechos en esta revisión (7/10): `ir-representation`,
`method-extraction`, `perform-thru`, `recursion-guard`, `traceability`, `nested-green`,
`regression-green`. Pendientes (3/10): `looping-variants`, `characterization`,
`carddemo-compiles` (ver secciones anteriores).