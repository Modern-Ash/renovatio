# Plan de implementación — issue #215

## Resultado

`generate`, `plan/apply` y la integración usada por la API consumirán el mismo resultado de
generación COBOL→Java. El cuerpo de servicio siempre atravesará el traductor semántico con el IR
base; el sidecar anotado sólo enriquecerá ese paso. Una generación que conserve el placeholder de
lógica de negocio fallará de forma explícita.

## Cambios

1. **Camino canónico en el proveedor COBOL**
   - Extraer en `JavaGenerationService` una única operación para traducir el cuerpo del servicio.
   - Pasar siempre el IR base, el archivo fuente, el colector de acciones manuales y, cuando exista,
     el contexto anotado y los `dataIntents` neutrales.
   - Rechazar el resultado si conserva el placeholder `TODO` de lógica COBOL.
   - Publicar en el `StubResult` agregado la ruta y la lista reales de artefactos escritos.

2. **Acciones manuales de IR no soportado**
   - Recorrer de forma determinista los párrafos del IR y convertir cada `UNTRANSLATED` en un
     `ManualActionItem` estable que incluya programa, fuente, párrafo y texto COBOL.
   - Fusionar esos ítems con los diagnósticos de resolución/anotaciones y escribir un único reporte.

3. **CLI `plan/apply`**
   - Eliminar la dependencia del directorio interno hardcodeado `generated-java-stubs`.
   - Validar y exponer la ubicación informada por `ApplyResult.changes.javaOutputDirectory`; con
     `--out`, el motor escribirá directamente en el destino pedido.
   - Añadir una prueba real `plan` → `apply --no-dry-run --out` que compruebe lógica traducida,
     ausencia del placeholder y equivalencia funcional con `generate`.

4. **Integración API/proveedor**
   - Añadir una prueba de ciclo `createMigrationPlan` → `applyMigrationPlan` sobre el proveedor real.
   - Verificar contenido Java, archivos generados y metadatos de salida, no sólo persistencia.

5. **Arquitectura y verificación**
   - Registrar un ADR con la cadena canónica y los límites legacy.
   - Ejecutar pruebas dirigidas de proveedor, recetas OpenRewrite, CLI y API, más el harness de
     caracterización afectado.
   - Registrar reporte y evidencia en Agora antes de solicitar aceptación del owner.

## Riesgos y mitigaciones

- **IR válido pero receta sin cambios:** la comprobación del placeholder convierte el silencio en un
  fallo observable.
- **Duplicación de acciones manuales:** IDs estables y mapa indexado por ID mantienen un solo ítem.
- **Diferencia de rutas entre CLI y motor:** el CLI consume `javaOutputDirectory` y falla si no
  coincide con `--out`.
- **Regresiones en perfiles/manifest:** se conserva `TargetEmitterRegistry` y se prueban las rutas
  existentes además del nuevo ciclo.

