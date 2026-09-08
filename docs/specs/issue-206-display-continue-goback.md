# Spec · #206 ciclo 1 — DISPLAY / CONTINUE / GOBACK / STOP RUN + sin drops silenciosos

- **Swarm:** `issue-206-display-continue-goback` (044)
- **Work item:** `display-continue-goback`
- **Issue:** https://github.com/Modern-Ash/renovatio/issues/206
- **Método:** spec-driven

## Contexto

`SimpleCobolIrParser.parseStatements` (módulo `renovatio-cobol-ir`) reconoce hoy: `IF`,
`EVALUATE`, `PERFORM`, `CALL`, `EXEC SQL`, file ops (`READ`/`WRITE`/`OPEN`/`CLOSE`), `COMPUTE`,
`ADD`/`SUBTRACT`/`MULTIPLY`/`DIVIDE`, `MOVE`. **Toda otra línea de PROCEDURE DIVISION se descarta
en silencio** (el `for` simplemente no hace `add`).

El reporte de cobertura CardDemo (#217) muestra que los verbos descartados más frecuentes son
`DISPLAY` (34 programas), `CONTINUE` (31), `GOBACK` (19), `STOP RUN` (2) — además de file I/O
(#213), `SET`, `INITIALIZE`, `STRING`, `SEARCH` que quedan para ciclos posteriores.

Consecuencia observable: `manualActionItems` = 0 en los 44 programas del reporte, porque lo que no
se traduce no deja rastro.

## Objetivo

1. Traducir `DISPLAY`, `CONTINUE`, `GOBACK`, `STOP RUN` a Java equivalente.
2. Que cualquier sentencia de PROCEDURE DIVISION que el traductor todavía no soporta quede
   **visible** como comentario `// <texto COBOL> — not translated yet` en el Java generado, en vez
   de desaparecer. Nunca emitir Java sintácticamente inválido por esto.

## Requisitos (SHALL)

- **R1 — DISPLAY.** `DISPLAY <operando>...` → `System.out.println(<expr>)` donde:
  - un literal `'texto'` / `"texto"` → string Java `"texto"`,
  - un identificador `FOO-BAR` → `input.getFooBar()` (mismo mapeo que usa el recipe hoy),
  - varios operandos → concatenación con `+` (`"literal " + input.getX() + input.getY()`),
  - `DISPLAY` sin operandos → `System.out.println()`.
  El `.` final y `UPON <dispositivo>` se ignoran (se documenta la simplificación).

- **R2 — CONTINUE.** `CONTINUE` → sentencia vacía explícita (`;` con comentario `// CONTINUE`), sin
  romper el bloque generado (p.ej. dentro de un `if (...) { }`).

- **R3 — GOBACK / STOP RUN.** `GOBACK`, `STOP RUN` y `STOP` a secas → `return <outputVar>;` en el
  punto donde aparecen (`outputVar` = la variable de salida que ya usa el render del
  párrafo/proceso). Si aparecen antes de otras sentencias del mismo bloque, el render **omite las
  sentencias posteriores de ese bloque** para no generar código Java inalcanzable. `STOP <literal>`
  (pausa del operador) → `UNTRANSLATED`.

- **R4 — Sin drops silenciosos.** Cualquier línea de PROCEDURE DIVISION no vacía que no sea
  cabecera de párrafo, marcador `END-*`, `EXIT`/`EXIT.` ni una de las sentencias soportadas, se
  representa en el IR como una sentencia "no traducida" y el recipe la emite exactamente como
  `// COBOL not translated: <texto COBOL recortado a 120 chars, sin el punto final>`. No se
  descarta y no produce tokens Java inválidos.

- **R5 — Java válido en el corpus.** Para los 44 programas del corpus CardDemo, el Java generado
  no tiene errores de **sintaxis** de `javac` atribuibles a este cambio (los errores semánticos
  preexistentes —símbolos no declarados, 88-levels— quedan fuera de alcance).

- **R6 — Delta de cobertura.** Se re-genera `docs/reports/carddemo-coverage.{md,json}`; `DISPLAY`,
  `CONTINUE`, `GOBACK` y `STOP RUN` salen de la tabla "constructos no traducidos"; el nº de
  programas que compilan **no baja de 10**.

- **R7 — Sin regresión ni pérdida de gate.** Los **tres** módulos `renovatio-cobol-ir`,
  `cobol-openrewrite-recipes` y `renovatio-provider-cobol` siguen verdes en `mvn verify` — los
  tres tienen gate jacoco de 100 % de línea, así que todo código nuevo en `src/main` de los tres
  debe quedar 100 % cubierto por tests nuevos. No se toca el mapeo de tipos,
  `MOVE`/`COMPUTE`/`IF`/`PERFORM`/`EVALUATE`/aritmética ni el emisor de DTOs.

## Criterios de aceptación (mapa a work item)

| Criterio | Requisitos |
| --- | --- |
| `display-rendered` | R1 |
| `continue-noop` | R2 |
| `goback-return` | R3 |
| `no-silent-drop` | R4, R5 |
| `coverage-delta` | R6 |
| `regression-green` | R7 |

## Fuera de alcance

- File I/O real (READ/WRITE/OPEN/CLOSE → puerto): #213.
- `SET`, `INITIALIZE`, `STRING`/`UNSTRING`/`INSPECT`, `SEARCH`, `GO TO`, `ACCEPT`: ciclos
  posteriores de #206.
- Semántica de consola (elegir logger vs `System.out` vs puerto): se usa `System.out.println`
  como traducción fiel para batch; la abstracción a puerto es una decisión posterior.
- Convertir el comentario `// not translated` en un `ManualActionItem` propagado al reporte de
  action items: deseable, pero la integración recipe→action-item se hace en un ciclo aparte para
  no arrastrar ese plumbing acá.

## Decisiones tomadas

- **Modelo IR:** un único record nuevo `SimpleStatement(Kind, String text)` con
  `Kind ∈ {DISPLAY, CONTINUE, GOBACK, STOP_RUN, UNTRANSLATED}`, implementando `CobolStatement`.
  Evita 5 records y 5 ramas nuevas.
- **`STOP RUN` vs `STOP`:** sólo `STOP RUN` (y `STOP` a secas) → `STOP_RUN`. `STOP <literal>`
  (pausa) → `UNTRANSLATED`.
- **Multilínea:** este ciclo trata `DISPLAY` de una sola línea lógica; un `DISPLAY` continuado en
  varias líneas físicas cae a `UNTRANSLATED` (documentado). Mejorar el lexer es otro ciclo.

## No ambigüedades pendientes

Ninguna material. El comportamiento está fijado por R1–R7 y por fixtures de caracterización nuevos.
