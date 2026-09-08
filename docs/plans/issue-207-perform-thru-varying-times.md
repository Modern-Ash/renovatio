# Plan de implementación — #207 PERFORM → métodos

## Cambios por módulo

### renovatio-cobol-ir
1. `PerformStatement`: añadir `testAfter` (boolean, default `false`) e `inlineBody` (`List<CobolStatement>`, default vacío) para `PERFORM ... END-PERFORM` inline; conservar constructores y validación.
2. `SimpleCobolIrParser.parsePerform`: soportar en una línea `THRU`, `n TIMES`, `UNTIL`, `VARYING x FROM a BY b UNTIL c` y `WITH TEST BEFORE/AFTER`.
3. `parseStatements`: tratar `PERFORM` como bloque multilínea — si la cláusula es inline, consumir cuerpo hasta `END-PERFORM` balanceado (anidando IF/EVALUATE/PERFORM inline); si la línea termina en `THRU` y la siguiente es un identificador sin punto, unirla (continuación real CardDemo).
4. `CobolIrIdentityProjector` + `cobol-ir.v1.schema.json`: serializar `testAfter` e `inlineBody`.

### cobol-openrewrite-recipes
5. `GeneratedFrom` (anotación local, emitida por aplicar) sobre cada método extraído: `@GeneratedFrom(paragraph = "<P>", source = "<archivo>:<líneas>")`. En fixtures el stub declara el `@interface` para compilar standalone.
6. `renderPerform` → emisión basada en llamadas:
   - `PERFORM p` → `performP(input, out);` (método extraído).
   - `PERFORM p THRU q` → secuencia ordenada de llamadas `performP...performQ`.
   - `PERFORM p n TIMES` → `for (int i = 0; i < n; i++) { performP(input, out); }`.
   - `PERFORM p UNTIL c` → `while (!(c)) { performP(input, out); }`; con `TEST AFTER` → `do { ... } while (!(c));`.
   - `PERFORM p VARYING x FROM a BY b UNTIL c` → `for (int x = a; !(c'); x += b) { performP(input, out); }` (c' reescribe la referencia local a x); variante `AFTER` → bucles anidados.
   - `PERFORM inline` → el cuerpo se renderiza como bloque en el punto de llamada.
7. Extracción de métodos en el visitante (nivel `visitClassDeclaration` post-super): cada párrafo del modelo distinto del párrafo raíz se emite como método privado `private void performX(Dto input, Dto out)` cuando es referenciado por algún `PERFORM`; firma fija `input`/`out` para determinismo.
8. Detección de ciclos: grafo de llamadas entre párrafos (incluye rangos THRU); nodos en SCC → no se generan llamadas, se emite comentario `// COBOL not translated: PERFORM cycle (<A> -> <B>)` y un `ManualActionItem` en el proveedor; la generación nunca recursiona infinito.

### renovatio-provider-cobol
9. `JavaGenerationService.collectUntranslatedStatements`: rama para ciclos PERFORM (`COBOL-PERFORM-CYCLE`), creando `ManualActionItem` estable con `constructionFamily = "PERFORM_CYCLE"`.
10. Fixtures de caracterización (nuevos): `perform-thru`, `perform-times`, `perform-until`, `perform-varying`, `perform-inline`; promover `perform-simple-nested` a SUPPORTED con `DISPLAY` observable (orden MAIN→OUTER→INNER vía stdout capturado en `CharacterizationFixture.run()`); generar goldens `expected.java` deterministas.
11. Test CardDemo: `COUSR00C.cbl` (PERFORM VARYING inline real) → el servicio emitido compila (`javac` exit 0) y el VARYING se renderiza como bucle.

## Firma de métodos extraídos

`private void perform<Pascal>(Dto input, Dto out)` — mutan el DTO vía setters y leen entrada vía getters, igual que el cuerpo raíz; GOBACK/STOP RUN dentro de un párrafo → `return;`. `out` fijo por determinismo; el llamador usa su propia variable local.

## Riesgos

- Generación idéntica byte-a-byte: se preserva el render actual del método raíz; los goldens existentes `move-numeric`, `initialize-set-level88` no cambian (no extraen métodos con PERFORM).
- `TEST AFTER` requiere token explícito; si no aparece se asume BEFORE (default COBOL).
- Condición VARYING compleja se traduce con el traductor de condiciones existente; la variable de control se reescribe a la local del bucle.
- Corpus CardDemo real con constructores fuera de alcance (GO TO, STRING...) cae en `ManualActionItem`, no bloquea la compilabilidad del programa elegido.
- Golden-master E2E se valida en #216; aquí se garantiza compilación del Java emitido.