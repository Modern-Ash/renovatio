# Especificación · #206 ciclo 2 — `INITIALIZE` y `SET` level-88

- **Swarm:** `issue-206-data-verbs-cycle-2` (046)
- **Work item:** `initialize-set-level88`
- **Issue:** https://github.com/Modern-Ash/renovatio/issues/206
- **Método:** spec-driven

## Objetivo

Traducir de forma determinista los dos verbos de datos más frecuentes que todavía no tienen IR
ejecutable: `INITIALIZE` y `SET condition-name TO TRUE/FALSE`. El pipeline debe producir Java
compilable para destinos elementales conocidos y conservar como acción manual cualquier variante
que no pueda resolver sin inventar semántica.

## Contrato

- `INITIALIZE A B` se representa con un nodo IR tipado y conserva el orden de destinos.
- Para un destino elemental, el valor inicial deriva de su `PicType`: espacios con longitud PIC
  para texto, cero del tipo Java correspondiente para números y `BigDecimal.ZERO` para decimales.
- `INITIALIZE ... REPLACING` y destinos de grupo sin estructura disponible quedan explícitamente
  no traducidos y generan un único `ManualActionItem` estable.
- `SET A B TO TRUE` resuelve cada condition-name level-88 a su padre y usa el primer valor declarado
  (el límite inferior si es un rango).
- `SET ... TO FALSE` elige de forma determinista un valor representable fuera del conjunto level-88;
  si no puede demostrarlo, conserva la sentencia como acción manual.
- Formas de `SET` para índices, punteros o `UP/DOWN BY` quedan fuera de este ciclo y son visibles como
  trabajo manual.
- Los nodos nuevos participan en la identidad estable del IR anotado.

## Aceptación

- Pruebas unitarias cubren parseo, validación, render por tipo, level-88 verdadero/falso y residuo.
- Fixtures productivos de caracterización generan y ejecutan Java esperado sin action items.
- La cobertura CardDemo se recalcula desde el pipeline y registra el delta contra la base posterior
  a #215: 32/44 emisiones y 802 action items observados en el reporte del 2026-09-08.
- Permanecen verdes `renovatio-cobol-ir`, `cobol-openrewrite-recipes` y
  `renovatio-provider-cobol`.

## Fuera de alcance

- Fidelidad completa de `ROUNDED`, `ON SIZE ERROR` y `COMP-3` (#208).
- `STRING`, `UNSTRING`, `INSPECT`, `SEARCH`, `GO TO` y `ACCEPT`, que siguen en #206.
- Inicialización recursiva de grupos/copybooks (#210).
- Puertos de File I/O, CICS, DB2 y llamadas (#211–#213).
