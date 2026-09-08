# Plan de implementación — #206 ciclo 2

## Cambios

1. Añadir `InitializeStatement` y `SetConditionStatement` al IR con colecciones inmutables,
   normalización de nombres y validaciones de constructor.
2. Reconocer sólo las formas soportadas en `SimpleCobolIrParser`; enviar las demás al fallback
   `UNTRANSLATED` existente.
3. Incluir ambos nodos en `CobolIrIdentityProjector` y sus tipos anotados.
4. Renderizar defaults PIC y valores level-88 en `PopulateCobolProcessRecipe`, usando comentarios
   `COBOL not translated` cuando el modelo no permita una traducción demostrable.
5. Extender el colector del proveedor para que esos residuos creen `ManualActionItem` estables.
6. Añadir fixtures de caracterización y pruebas unitarias; recalcular cobertura CardDemo.

## Riesgos

- Un nombre de grupo no aparece como item elemental: se conserva como acción manual.
- `FALSE` puede no tener un valor alternativo demostrable: se intenta un conjunto finito y tipado;
  sin candidato seguro no se genera asignación.
- La generación secuencial todavía usa el DTO de entrada como fuente; este ciclo no redefine la
  memoria de ejecución ni adelanta #208/#210.
