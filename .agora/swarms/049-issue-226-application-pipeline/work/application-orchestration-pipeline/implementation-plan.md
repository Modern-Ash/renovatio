---
schema: "agora/implementation-plan/v1"
work-item: "application-orchestration-pipeline"
swarm: "issue-226-application-pipeline"
revision: 1
status: "planned"
---

# Plan de implementación — AC-05

## 1. Frontera application sin frameworks

- Crear `renovatio-application` como módulo Java sin Spring, transportes ni filesystem.
- Definir los nueve casos de uso públicos y los puertos mínimos de análisis, propuestas,
  proyección, emisión, refinamiento, validación, proyectos, artifacts, Git, red y reloj.
- Modelar snapshot, manifest, ChangeSet, comandos, resultados y errores tipados como valores
  inmutables y canónicos.

## 2. Pipeline único

- Implementar una fachada única que coordine create/analyze, review/resolve, plan, preview,
  validate, apply y export evidence.
- Compartir el mismo snapshot y manifest entre preview, validate y apply.
- Implementar digest determinista, precondiciones stale e idempotencia por
  `(project, use-case, key)`.

## 3. Apply atómico y recuperable

- Preparar preimage/postimage antes de publicar.
- Publicar mediante un repositorio de artifacts transaccional.
- Ante cualquier fallo, restaurar el preimage y persistir un ChangeSet `REVERTED`; sólo publicar
  `APPLIED` después de materialización, validación y Git local compensable.
- Mantener red/push remoto fuera de apply.

## 4. Adaptadores productivos

- Añadir un adapter para providers legacy detrás de los ports application.
- Encauzar CLI y MCP por la misma fachada; conservar DTOs y nombres externos.
- Encauzar las superficies API de plan/preview/validate/apply por la misma frontera mediante
  configuración Spring, sin decisiones de migración en controllers.

## 5. Contratos y verificación

- Crear un test kit reusable para repositorios/puertos.
- Cubrir determinismo, ausencia de writes en plan/preview, paridad del manifest, replay/conflicto
  idempotente, stale source/manifest y rollback con fault injection.
- Añadir regla arquitectónica que impida dependencias de frameworks/transportes en application.
- Ejecutar tests del módulo y reactor afectado; registrar toolchain, commit y resultados en Agora.

## Secuencia y checkpoints

1. Contratos y modelos; tests de invariantes.
2. Orquestador y test kit; tests de pipeline y atomicidad.
3. Adaptadores API/CLI/MCP y compatibilidad.
4. Documentación arquitectónica, verificación completa y evidencia Agora.

## Estrategia de rollback

Cada adapter conserva su contrato externo. El adapter legacy queda detrás de los nuevos ports y se
puede retirar por módulo si falla una verificación; no se introduce routing productivo paralelo.

