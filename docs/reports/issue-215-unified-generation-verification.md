# Verificación · issue #215 — Pipeline canónico COBOL→Java

- Commit verificado: `c254565e4d3961a26ca79eb0db297da1a1e64d94`
- Fecha: 2026-09-08
- Resultado: aprobado

## Cobertura de aceptación

- El proveedor hace pasar toda implementación por `translateServiceImplementation`, aun sin
  sidecar anotado, y falla si sobrevive el placeholder de lógica.
- `generate` y `plan`/`apply --no-dry-run --out` producen el mismo `ServiceImpl`; la prueba compara
  el contenido byte a byte y confirma `MOVE`, `DISPLAY` y ausencia del `TODO`.
- El ciclo persistente usado por API comprueba plan, run, archivos, metadatos y contenido Java real.
- Una sentencia `UNTRANSLATED` produce exactamente un `ManualActionItem` estable entre ejecuciones.
- El ADR 0002 registra la cadena canónica y los límites de los componentes legacy.

## Pruebas ejecutadas

| Alcance | Comando | Resultado |
| --- | --- | --- |
| Proveedor y caracterización dirigida | `mvn -pl renovatio-provider-cobol -Dtest=JavaGenerationServiceTest,JavaGenerationServiceAnnotatedTest,JavaGenerationRegistryRoutingTest,CobolSemanticTranspilerTest,CharacterizationFixtureContractTest test -q` | Éxito; 27 pruebas, 0 fallos |
| Provider COBOL completo | `mvn -pl renovatio-provider-cobol test -q` | Éxito |
| CLI plan/apply real | `mvn -pl renovatio-cli -am -Dtest=ApplyCommandTest -Dsurefire.failIfNoSpecifiedTests=false test -q` | Éxito; 3 pruebas, 0 fallos |
| Regresión CLI seleccionada | `mvn -pl renovatio-cli -am -Dtest=AnalyzeCommandTest,ApplyCommandTest,DiffCommandTest,GenerateCommandTest,MetricsCommandTest,PlanCommandTest,RenovatioCliSmokeTest,ReportCommandTest,ReusableCommandsTest,ReviewCommandTest,ServeCommandTest -Dsurefire.failIfNoSpecifiedTests=false test -q` | Éxito |
| Ciclo API persistente | `mvn -pl renovatio-api -am -Dtest=FullJobLifecycleTest -Dsurefire.failIfNoSpecifiedTests=false -Dexec.skip=true test -q` | Éxito; 3 pruebas, 0 fallos |
| Regresión API seleccionada | `mvn -pl renovatio-api -am -Dtest=ApiAccessServiceTest,ArchitecturePreviewApiTest,DataAccessServiceTest,DecisionLayerApiTest,FullJobLifecycleTest,LegacyProjectProfileImporterTest,ProjectRepositoryTest,ReusableAssetsApiTest,SseEventCollectorTest,WorkbenchArchitectureCanvasServiceTest,WorkbenchChangeSetApiTest,WorkbenchContextServiceTest,WorkbenchDomainModelApiTest,WorkbenchDomainModelServiceTest,WorkbenchEquivalenceLabApiTest,WorkbenchEquivalenceServiceTest,WorkbenchProjectAdapterServiceTest,WorkbenchSourceExplorerServiceTest -Dsurefire.failIfNoSpecifiedTests=false -Dexec.skip=true test -q` | Éxito |
| Recetas OpenRewrite | `mvn -pl cobol-openrewrite-recipes test -q` | Éxito |
| Higiene del diff | `git diff --check` | Éxito |

`-Dexec.skip=true` desactiva la ejecución de build frontend que requiere un binario local `vite`;
no desactiva Surefire ni las pruebas backend. La ejecución sin esa opción falló únicamente al no
encontrar `vite`, antes de añadir o detectar fallos Java.

## Revisión

No se detectaron regresiones funcionales ni discrepancias entre las rutas de generación. El nombre
por defecto `generated-java-stubs` permanece sólo como compatibilidad cuando no se solicita un
destino; el CLI ya no lo usa como contrato. Los avisos existentes de versiones ANTLR distintas se
mantienen sin cambio y no producen fallos.
