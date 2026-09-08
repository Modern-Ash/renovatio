# Plan · #206 ciclo 1 — DISPLAY / CONTINUE / GOBACK / STOP RUN + sin drops silenciosos

- **Spec:** `docs/specs/issue-206-display-continue-goback.md`
- **Rama:** `agora/issue-206-control-io-verbs` (worktree `/tmp/renovatio-215`, base `main`)

## Archivos

| Módulo | Archivo | Cambio |
| --- | --- | --- |
| `renovatio-cobol-ir` | `model/SimpleStatement.java` | **nuevo** record `SimpleStatement(Kind kind, String text)`, `enum Kind {DISPLAY, CONTINUE, GOBACK, STOP_RUN, UNTRANSLATED}` |
| `renovatio-cobol-ir` | `parser/SimpleCobolIrParser.java` | dispatch de `DISPLAY`/`CONTINUE`/`GOBACK`/`STOP` + fallback `UNTRANSLATED` en `parseStatements` |
| `cobol-openrewrite-recipes` | `PopulateCobolProcessRecipe.java` | rama `instanceof SimpleStatement` en `renderStatement`; corte de sentencias tras `return` |
| tests | `SimpleCobolIrParserTest` (o nuevo `SimpleStatementParsingTest`) | 100 % de las ramas nuevas del parser |
| tests | `PopulateCobolProcessRecipeTest` | 100 % de las ramas nuevas del recipe |
| fixtures | `renovatio-provider-cobol/src/test/resources/characterization/display-continue-goback/` | `input.cob` + `expected.java` + jsons; alta en `CharacterizationFixtureContractTest` (¿`SUPPORTED`?) |
| reporte | `docs/reports/carddemo-coverage.{md,json}` | regenerado |

## Paso 1 — IR: `SimpleStatement` (renovatio-cobol-ir)

```java
public record SimpleStatement(Kind kind, String text) implements CobolStatement {
    public enum Kind { DISPLAY, CONTINUE, GOBACK, STOP_RUN, UNTRANSLATED }
    public SimpleStatement {
        Objects.requireNonNull(kind, "kind");
        text = text == null ? "" : text.strip();
    }
}
```

## Paso 2 — Parser (`SimpleCobolIrParser.parseStatements`)

Antes del último `if (upperLine.startsWith(MOVE))`, añadir:

- `startsWith("DISPLAY")` → `new SimpleStatement(DISPLAY, línea sin 'DISPLAY' ni '.' final)`.
- `equals("CONTINUE")` / `startsWith("CONTINUE")` → `SimpleStatement(CONTINUE, "")`.
- `equals("GOBACK")` / `startsWith("GOBACK")` → `SimpleStatement(GOBACK, "")`.
- `equals("STOP RUN")` / `equals("STOP")` → `SimpleStatement(STOP_RUN, "")`.
  `startsWith("STOP ")` con resto ≠ `RUN` → `UNTRANSLATED`.
- Nuevo **fallback** al final del `for` (reemplaza el "no hacer nada"): si la línea no está vacía y
  no es `EXIT`/`EXIT.`/`.`/`END-*`/cabecera de párrafo (regex `^[A-Z][A-Z0-9-]*\.$` ya cubierta
  aguas arriba) → `new SimpleStatement(UNTRANSLATED, línea)`.
  - Filtro explícito de ruido: `EXIT`, `EXIT.`, `.`, `END-IF`, `END-EVALUATE`, `END-PERFORM`,
    `END-STRING`, `END-READ`, `END-EXEC`.

Sub-parseo de `DISPLAY`: separar el resto de la línea en tokens respetando comillas simples/dobles.
`'a b' X 'c'` → `["'a b'", "X", "'c'"]`. Guardar el texto crudo del resto en `text` y que el
**recipe** haga el split→expresión (mantiene la lógica de mapeo de identificadores en un solo
lugar, reutilizando `toJavaExpression`).

## Paso 3 — Recipe (`PopulateCobolProcessRecipe.renderStatement`)

```java
if (statement instanceof SimpleStatement simple) {
    return switch (simple.kind()) {
        case CONTINUE      -> List.of("; // CONTINUE");
        case GOBACK, STOP_RUN -> List.of(returnVar(varName) );      // "return out;" / "return <varName>;"
        case DISPLAY       -> List.of("System.out.println(" + displayArgs(simple.text()) + ");");
        case UNTRANSLATED  -> List.of("// COBOL not translated: " + truncate(simple.text(), 120));
    };
}
```

- `displayArgs("")` → `""` (queda `System.out.println();`).
- `displayArgs` divide el texto en literales entre comillas y otros tokens; literal → `toJavaExpression("'..'")`; token identificador → `toJavaExpression(token)`; se unen con `" + "`.
- **Corte tras return:** en `renderParagraph` / bucle que acumula líneas de un bloque, si una
  sentencia renderiza `return ...;`, no renderizar las siguientes de ese mismo bloque (evita
  "unreachable statement"). Implementar en el punto donde se itera `paragraph.statements()` y en
  `renderIf` (then/else).

## Paso 4 — Fixtures y tests

- `characterization/display-continue-goback/input.cob`: un programa mínimo con
  `DISPLAY 'HELLO' WS-NAME`, `CONTINUE` dentro de un `IF`, y `GOBACK`.
- `expected.java` con el `System.out.println("HELLO" + input.getWsName())`, el `; // CONTINUE` y el
  `return`.
- Unit tests directos del parser (cada `Kind`, cada rama del fallback y del filtro de ruido) y del
  recipe (cada `case` del switch + corte tras return) para llegar a 100 % en los dos módulos.
- Revisar si `display-continue-goback` entra en `CharacterizationFixtureContractTest.SUPPORTED`
  (sólo si el pipeline lo traduce end-to-end sin `// TODO`).

## Paso 5 — Regenerar reporte + verificación

```bash
mvn -o -q install -pl renovatio-cobol-ir,cobol-openrewrite-recipes -DskipTests -Dexec.skip=true
mvn -o test -pl renovatio-provider-cobol -Dgroups=coverage -Drenovatio.surefire.excludedGroups= -Dexec.skip=true
mvn -o verify -pl renovatio-cobol-ir,cobol-openrewrite-recipes,renovatio-provider-cobol -Dexec.skip=true
```

`verification-report`: delta del reporte (DISPLAY/CONTINUE/GOBACK/STOP RUN fuera de la lista;
compilan ≥ 10), resultado de los 3 `verify` con gate jacoco, y confirmación de que no se tocó
MOVE/COMPUTE/aritmética/DTO.

## Riesgos

- **Cobertura 100 % × 3 módulos:** cada rama nueva necesita un test. Es el grueso del esfuerzo.
- **Corte tras return:** hay que aplicarlo en todos los puntos que iteran statements (párrafo, IF,
  EVALUATE, PERFORM inline). Si se olvida uno → `unreachable statement` en algún programa CardDemo.
- **`EXIT` como cabecera vs sentencia:** `EXIT.` al final de un párrafo hoy se ignora; mantener ese
  comportamiento (no convertirlo en `UNTRANSLATED`).
- **DISPLAY multilínea:** cae a `UNTRANSLATED` (aceptado por la spec).

## Commits

1. `feat(cobol-ir): model DISPLAY/CONTINUE/GOBACK/STOP and untranslated statements (#206)`
2. `feat(cobol): render DISPLAY/CONTINUE/GOBACK + surface untranslated statements (#206)`
3. `test(cobol): characterization fixture for DISPLAY/CONTINUE/GOBACK (#206)`
4. `docs(reports): refresh CardDemo coverage after #206 cycle 1`
5. `docs(agora): verification report for #206 cycle 1`
