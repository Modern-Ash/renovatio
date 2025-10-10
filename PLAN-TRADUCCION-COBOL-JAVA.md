# Plan para implementar la traducción real de COBOL a Java

## 1. Estado actual del generador COBOL → Java

- Los servicios generados a partir de plantillas incluyen solo comentarios `TODO` en los puntos donde debería migrarse la lógica de negocio (`TemplateCodeGenerationService`).【F:renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/service/TemplateCodeGenerationService.java†L41-L80】
- Las clases construidas con JavaPoet crean métodos `process` y `validate` sin implementar la semántica original del programa COBOL.【F:renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/service/JavaGenerationService.java†L200-L250】
- El adaptador de programas COBOL mantiene excepciones `UnsupportedOperationException`, lo que evidencia la falta de integración con la lógica real migrada.【F:renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/CobolProvider.java†L381-L407】

## 2. Objetivo de la mejora

Construir una canalización que transforme la **semántica** de los párrafos COBOL (procedures, condiciones `IF/EVALUATE`, sentencias `PERFORM`, accesos a archivos/DB2, etc.) en código Java funcional, generando servicios Spring con reglas de negocio reales y pruebas automatizadas.

## 3. Componentes técnicos necesarios

### 3.1. Extracción semántica del COBOL

1. **Parser enriquecido**: utilizar `ProLeap` (ya presente como dependencia) con listeners propios para extraer:
   - División `DATA`: tipos, estructuras, niveles, `PIC`, `REDEFINES`, `OCCURS`.
   - División `PROCEDURE`: párrafos, secciones, sentencias, flujo de control.
2. **Árbol intermedio (IR)**: definir un modelo `CobolIntermediateModel` que normalice:
   - Nodos de control (`IfNode`, `EvaluateNode`, `PerformNode`).
   - Operaciones aritméticas y de movimientos (`MoveNode`, `ComputeNode`).
   - Interacciones externas (`CallNode`, `Db2Node`, `FileNode`).
3. **Análisis de flujo**: construir un grafo de flujo de control (CFG) para resolver `PERFORM THRU`, `GO TO`, `EXIT`, garantizando orden de ejecución para la traducción.

### 3.2. Motor de traducción alineado con OpenRewrite

1. **Recipes OpenRewrite personalizadas**: definir un módulo `cobol-openrewrite-recipes` donde cada nodo del IR se materialice como una `Recipe` de OpenRewrite que parte del stub generado y lo enriquece con la lógica real (por ejemplo `CobolPerformRecipe`, `CobolEvaluateRecipe`). Estas recetas deberán construir o modificar `J.MethodDeclaration`, `J.If`, `J.Switch`, etc., para garantizar un AST Java válido.
2. **Infraestructura compartida**: reutilizar directamente `OpenRewriteRunner` y el ciclo de ejecución existente (`JavaRecipeExecutor`) para aplicar las nuevas recetas a los servicios generados, asegurando la misma interfaz MCP (`preview/apply`, métricas, validaciones de seguridad).【F:renovatio-provider-java/src/main/java/org/shark/renovatio/provider/java/OpenRewriteRunner.java†L12-L132】【F:renovatio-provider-java/src/main/java/org/shark/renovatio/provider/java/execution/JavaRecipeExecutor.java†L23-L169】
3. **Descubrimiento y composición**: registrar las recetas COBOL en `OpenRewriteRecipeDiscoveryService` para exponerlas como herramientas MCP al igual que las de Java, aprovechando la activación dinámica, validaciones y marcado de recetas inseguras.【F:renovatio-provider-java/src/main/java/org/shark/renovatio/provider/java/discovery/OpenRewriteRecipeDiscoveryService.java†L22-L158】
4. **Conversión de tipos y estado**: mantener una tabla `PIC` → tipos Java y un contexto de ejecución (`CobolExecutionContext`) que puedan inyectarse a las recetas para generar atributos, DTOs y parámetros adecuados.
5. **Soporte DB2/archivos**:
   - Incluir recetas específicas para construir repositorios Spring Data/JDBC a partir de metadata COBOL.
   - Generar adaptadores de I/O mediante recetas que creen componentes Spring (`@Repository`, `@Component`) con la configuración necesaria.

### 3.3. Integración con Spring y la capa de servicios

1. Generar **servicios transaccionales** (`@Service`, `@Transactional`) que invoquen los bloques traducidos.
2. Crear **componentes reutilizables** para:
   - Conversión de copybooks a DTO (`CopybookMapper` con MapStruct).
   - Gestión de errores (`CobolRuntimeException`, `CobolConditionHandler`).
3. Implementar **adaptadores** que repliquen las llamadas externas: CICS, MQ, web services, etc. Bibliotecas de referencia:
   - `LegStar` para llamadas CICS/IMS.
   - `Jackson`/`MapStruct` para serialización.
   - `Spring Integration` para colas/mensajería.

### 3.4. Configuración de la versión objetivo de Java

1. **Versión unificada**: establecer Java 17 como target para los servicios traducidos, alineado con el stack actual del proyecto.
2. **OpenRewrite HasMinimumJavaVersion**: actualizar la configuración YAML para las nuevas recetas COBOL e incluir explícitamente el target (`version: '17'`) siguiendo el patrón ya usado por el proveedor Java.【F:renovatio-provider-java/rewrite.yml†L1-L7】
3. **Generación de código**: propagar la versión destino al generador (JavaPoet, plantillas Freemarker y validaciones) para asegurar compatibilidad con records, `var` y APIs de Java 17 cuando aplique.

### 3.5. Generación de pruebas automatizadas

1. Usar el IR para producir casos de prueba JUnit:
   - Inputs/outputs derivados de las reglas `MOVE`, `IF`, `EVALUATE`.
   - Fixtures generados a partir de `COPY` y ejemplos de datos.
2. Validar equivalencia ejecutando el programa COBOL original (si es posible) vía `GnuCOBOL` o servicios mockeados y comparando resultados.

## 4. Flujo propuesto

1. **Parsing** → `CobolParserService` produce IR.
2. **Normalización** → Resuelve `PERFORM` y estructura el CFG.
3. **Traducción** → Módulo de reglas genera AST Java (`JavaPoet`/`OpenRewrite`).
4. **Ensamblado** → Plantillas Freemarker + clases generadas integran DTO, servicios, adaptadores.
5. **Testing** → Se crean pruebas unitarias/integración y se ejecutan en Maven.

## 5. Roadmap incremental

| Fase | Entregables clave |
|------|-------------------|
| Fase 1 | Definición del IR, listeners ProLeap, primeras recetas OpenRewrite (`MOVE`, `IF`, `PERFORM`) ejecutadas con el `OpenRewriteRunner`. |
| Fase 2 | Soporte para `EVALUATE`, `STRING/UNSTRING`, aritmética completa, generación de DTOs enriquecidos y registro en `OpenRewriteRecipeDiscoveryService`. |
| Fase 3 | Integración DB2/archivos, recetas para repositorios y servicios Spring transaccionales. |
| Fase 4 | Traducción de llamadas externas (CICS/MQ), adaptadores configurables, pruebas automáticas generadas. |
| Fase 5 | Optimización y validación (comparación con ejecución COBOL, perfilado de rendimiento). |

## 6. Herramientas y librerías recomendadas

- **ProLeap** o **Koopa**: parsing COBOL y obtención de AST detallado.
- **OpenRewrite** + **JavaPoet**: generación y refactorización de código Java.
- **MapStruct**: mapeo automático de DTO ↔ estructuras COBOL.
- **jOOQ** / **Spring Data JDBC**: traducción de sentencias DB2 a repositorios Java.
- **LegStar** / **Spring Integration**: integración con CICS, MQ y servicios externos.
- **GnuCOBOL** (en pipelines CI) para validar resultados ejecutando el programa original.
- **ANTLR** (opcional) para reglas específicas cuando ProLeap no cubra ciertas extensiones.

## 7. Próximos pasos inmediatos

1. Crear el módulo `renovatio-cobol-ir` con el modelo intermedio y pruebas sobre un COBOL de ejemplo.
2. Construir el módulo `cobol-openrewrite-recipes` reutilizando `OpenRewriteRunner` para validar las primeras recetas (`MOVE`, `COMPUTE`, `IF`, `PERFORM`).
3. Registrar las recetas en `OpenRewriteRecipeDiscoveryService` y exponer herramientas MCP (`cobol.<recipeId>`) con la misma interfaz que `java.apply`/`java.analyze`.
4. Configurar la versión objetivo Java 17 en las recetas YAML y propagarla al generador.
5. Integrar generación de pruebas unitarias que comparen DTO antes/después de la traducción.
6. Documentar la extensión en `ARCHITECTURE.md` y preparar demos con programas COBOL reales.

Este plan permitirá evolucionar de esqueletos a servicios Java funcionales respetando la lógica original de los programas COBOL.
