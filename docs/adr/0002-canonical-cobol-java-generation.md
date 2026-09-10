# ADR 0002: Pipeline canónico de generación COBOL→Java

- Estado: aceptado
- Fecha: 2026-09-08
- Issue: #215

## Contexto

La generación Java tenía tres límites que parecían fuentes de verdad independientes:

- `JavaGenerationService` construía DTO, interfaz e implementación mediante JavaPoet;
- `JavaEmitter` y `JavaArtifactRenderer` resolvían el target y el manifest de arquitectura;
- `PopulateCobolProcessRecipe` insertaba la lógica del IR en el cuerpo Java.

La generación directa alcanzaba esos componentes, pero `plan/apply` no recibía los metadatos del
resultado agregado y el CLI copiaba desde `generated-java-stubs`. Además, una ejecución de
OpenRewrite sin cambios podía conservar el placeholder del template y reportarse como exitosa.

## Decisión

La cadena productiva única es:

```text
fuente COBOL
  → CobolParsingService / CobolIntermediateModel
  → CobolSemanticProjector
  → DecisionResolver + ArchitectureTransformer (IR→IR)
  → TargetEmitterRegistry / JavaEmitter (selección y manifest)
  → JavaGenerationService (esqueleto Java único)
  → CobolSemanticTranspiler / PopulateCobolProcessRecipe (cuerpo único)
  → GeneratedArtifactTreeWriter
  → StubResult(outputPath, generatedFiles)
```

`JavaGenerationService.translateServiceImplementation` es el único punto del camino Java que
decide y valida el cuerpo de `process`. Siempre invoca `CobolSemanticTranspiler` con el IR base. Un
`AnnotatedCobolContext` válido y los `dataIntents` sólo enriquecen esa ejecución; su ausencia nunca
desactiva la traducción determinista.

Si el resultado conserva `// TODO: Implement COBOL business logic`, la generación falla cerrada.
Los `SimpleStatement.UNTRANSLATED` permanecen visibles en Java y producen, además, un
`ManualActionItem` determinista en el reporte único del workspace.

`MigrationPlanService` propaga la ruta y los archivos informados por `StubResult`. `ApplyCommand`
consume `changes.javaOutputDirectory`; con `--out`, el motor escribe directamente allí y el CLI
valida la ruta en vez de copiar desde un directorio interno.

## Límites y componentes legacy

- El nombre por defecto `generated-java-stubs` se conserva temporalmente por compatibilidad cuando
  el llamador no pasa `outputDir`; deja de ser un contrato del CLI.
- `generateInterfaceStubsLegacy` conserva el armado JavaPoet mientras se migra el renderer, pero ya
  no constituye un pipeline alternativo: todas sus implementaciones de servicio pasan por la
  función canónica antes de emitirse.
- `JavaEmitter` no genera cuerpos: adapta el resultado canónico al SPI, aplica selección de target y
  valida/rebasa el manifest de arquitectura.
- El harness puede ejercitar `PopulateCobolProcessRecipe` directamente para caracterización, pero
  producción llega a la misma receta a través de `CobolSemanticTranspiler`.

## Consecuencias

- `generate`, API y `plan/apply` producen el mismo mapa de artefactos para igual fuente, perfil y
  destino lógico.
- Una regresión de integración deja un error observable en vez de Java compilable con un TODO.
- El trabajo manual por sintaxis aún no soportada queda auditable y estable.
- Una futura sustitución de JavaPoet o del nombre del directorio sólo debe preservar el contrato de
  `StubResult`, no lógica de copia en los adaptadores.
