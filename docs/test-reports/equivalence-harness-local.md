# Equivalence harness — verificación local

Fecha: 2026-09-05  
Trabajo Agora: `equivalence-validation-rework/cobol-target-equivalence`

## Alcance

La verificación cubre el contrato del harness, comparación estructural, gate de equivalencia,
persistencia del historial y configuración de runners externos. No afirma equivalencia de un
sistema COBOL productivo: para eso deben conectarse los ejecutables reales mediante
`RENOVATIO_BASELINE_BIN` y `RENOVATIO_CANDIDATE_BIN`.

## Comandos ejecutados

```text
mvn -q -pl renovatio-domain-model,renovatio-llm -am test
mvn -q -pl renovatio-api -am -DskipTests compile
mvn -q -pl renovatio-provider-java clean test -DfailIfNoTests=false
```

Resultado: `PASS` para los módulos y pruebas indicados.

## Evidencia funcional disponible

- `POST /api/projects/{projectId}/equivalence/compare`: compara baseline/candidate.
- `POST /api/projects/{projectId}/equivalence/gate`: aplica el umbral configurable.
- `POST /api/projects/{projectId}/equivalence/replay`: ejecuta ambos runners y persiste el
  resultado.
- `GET /api/projects/{projectId}/equivalence/history`: devuelve ejecuciones anteriores.
- `scripts/replay-baseline.sh` y `scripts/replay-candidate.sh`: wrappers reproducibles para
  configurar los ejecutables sin hardcodear rutas.

## Pendiente para evidencia de negocio

1. Registrar fixtures representativos de cada proceso COBOL crítico.
2. Configurar los binarios baseline y candidate en un entorno controlado.
3. Ejecutar replay completo y conservar respuestas, divergencias y versión de artefactos.
4. Emitir el `review-report` con aprobación humana antes del cutover.
