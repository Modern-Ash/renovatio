# Investigación sobre librerías y frameworks para mapear COBOL → Java

Este documento resume alternativas para implementar la traducción de primitivas, estructuras de datos y rutinas COBOL hacia código Java funcional, manteniendo el esquema de interfaces ya disponible en Renovatio (basado en OpenRewrite + servicios Spring generados).

## 1. Requerimientos clave

- **Cobertura de tipos básicos**: soportar `PIC` (alfanuméricos, numéricos, zoned, packed) y cláusulas `REDEFINES`, `OCCURS`, `COMP-*`.
- **Conversión de rutinas comunes**: sentencias `MOVE`, `COMPUTE`, `STRING/UNSTRING`, evaluaciones `IF/EVALUATE`, `PERFORM`.
- **Integración con OpenRewrite**: exponer la lógica de conversión como `Recipe`/`Visitor` para inyectar código Java dentro de los stubs generados.
- **Generación de DTOs y servicios**: producir POJOs/records, mappers y servicios Spring con lógica real, reemplazando los `@TODO` actuales.

## 2. Librerías y frameworks relevantes

| Herramienta | Tipo | Aportes para la migración | Observaciones |
|-------------|------|---------------------------|---------------|
| **JRecord** | Open Source (Apache 2.0) | Lee copybooks COBOL, genera POJOs Java y provee conversiones `PIC` ↔ tipos Java, incluyendo packed/comp-3. Útil para mapear la división `DATA` a DTOs reutilizables desde OpenRewrite. | Integración directa posible: el IR COBOL puede usar JRecord para resolver metadata y la receta Java generar clases con el mismo layout. |
| **LegStar** | Open Source (LGPL) | Toolkit para binding Java ↔ COBOL (incluye generador `cob2j`), serialización/deserialización de estructuras y runtime para CICS/IMS. | Permite generar clases Java a partir de copybooks y manejar conversiones de carácter/decimal. Se puede invocar durante la fase de generación y envolver la lógica dentro de recetas OpenRewrite. |
| **OpenLegacy** | Comercial con componentes OSS | Plataforma de modernización que expone programas COBOL como servicios Java/REST generados. | Referencia arquitectónica: sugiere pipelines donde un parser produce modelos y se inyectan en plantillas. Podemos replicar el patrón con OpenRewrite y nuestro IR. |
| **Heirloom Platform** | Comercial | Traduce COBOL a Java gestionando runtime y tipos. | Inspiración sobre cómo mapear instrucciones (`PERFORM`, `CALL`). No reutilizable directamente, pero valida la viabilidad técnica del mapeo completo. |
| **Raincode COBOL Compiler** | Comercial | Compila COBOL a .NET/Java conservando semántica. | Otra referencia sobre el alcance que debe cubrir el motor de traducción. |
| **Micro Focus Enterprise COBOL + JVM** | Comercial | Ofrece generación de bytecode Java y API de interoperabilidad. | Puede aportar guías de conversión de tipos (`PIC` → `BigDecimal`, `String`). |

## 3. Cómo integrar estas soluciones con OpenRewrite

1. **Normalización con IR**: continuar con `renovatio-cobol-ir` para representar nodos semánticos. Aprovechar JRecord/LegStar para resolver tipos antes de generar AST Java.
2. **Recetas específicas**: crear recetas en `cobol-openrewrite-recipes` como `CobolDataDivisionRecipe`, `CobolPerformRecipe`, `CobolEvaluateRecipe`. Cada receta implementa `Recipe` y usa `JavaIsoVisitor` para insertar o modificar `J.MethodDeclaration`, `J.If`, `J.Block` respetando el esquema MCP existente (`preview/apply`).
3. **Inyección de mappers**: con MapStruct (ya en el stack) generar interfaces `@Mapper` que traduzcan las estructuras JRecord/LegStar a DTOs y viceversa, y referenciarlas desde las recetas mediante plantillas JavaPoet o `Template` de OpenRewrite.
4. **Servicios funcionales**: reemplazar los `@TODO` mediante la composición de recetas que ensamblen la lógica (por ejemplo, `CobolPerformRecipe` crea métodos privados por párrafo COBOL y `CobolServiceAssemblerRecipe` conecta esos métodos en el `process`).

## 4. Consideraciones adicionales

- **Compatibilidad de licencias**: JRecord (Apache) y LegStar (LGPL) son integrables; las soluciones comerciales pueden servir como guía, pero no se pueden distribuir dentro del repositorio.
- **Pruebas**: generar pruebas JUnit apoyadas en datos de ejemplo (`copybooks` + `flat files`) usando JRecord para validar la equivalencia entre COBOL y Java.
- **Ejecución en pipelines**: integrar el flujo dentro del actual `OpenRewriteRunner` para mantener métricas, dry-runs (`preview`) y capacidad MCP.

## 5. Próximos pasos propuestos

1. Prototipo con JRecord para convertir copybooks a POJOs y verificar el mapping de tipos numéricos/packed.
2. Exponer el prototipo como receta OpenRewrite que inyecta DTOs y métodos en un servicio generado.
3. Evaluar LegStar para llamadas externas (CICS/IMS) y para ampliar la cobertura de conversiones.
4. Documentar el contrato MCP para recetas COBOL (`tools/list`, `tools/describe`) asegurando compatibilidad con los ejecutores actuales.

Esta estrategia permite cubrir las primitivas y rutinas COBOL con código Java real y reutilizable, manteniendo la arquitectura basada en OpenRewrite.
