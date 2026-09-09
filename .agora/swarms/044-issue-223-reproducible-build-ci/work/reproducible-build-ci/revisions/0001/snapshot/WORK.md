---
schema: "agora/work/v1"
id: "reproducible-build-ci"
swarm: "issue-223-reproducible-build-ci"
title: "Build reproducible y CI obligatoria de producto completo (#223)"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"single-bootstrap":"Un clone limpio dispone de una orden documentada que instala toolchains/dependencias bloqueadas por lockfile y ejecuta el build completo sin estado previo de node_modules.","maven-green":"El reactor Maven completo termina verde, incluyendo Node, CLI, API y renovatio-llm-evals.","node-collision":"La generación Node multiprograma no produce paths duplicados para docs/node-idioms.md y conserva determinismo del manifest.","versions-pinned":"Java, Maven plugins incluido JaCoCo, Node/package manager y Python están fijados o validados explícitamente.","ci-matrix":"CI ejecuta Maven, renovatio-ui, renovatio-workbench, Python, caracterización y equivalencia, con fallos de seguridad relevantes no tolerados mediante ignore incondicional.","clean-clone-proof":"La evidencia proviene de un clone temporal limpio sin targets ni node_modules heredados.","developer-parity":"Las mismas órdenes funcionan localmente y en CI y están documentadas en el README de desarrollo."}
satisfied-criteria: ["single-bootstrap","maven-green","node-collision","versions-pinned","ci-matrix","clean-clone-proof","developer-parity"]
criterion-statuses: {"single-bootstrap":["specified","planned","implemented","verified","accepted"],"maven-green":["specified","planned","implemented","verified","accepted"],"node-collision":["specified","planned","implemented","verified","accepted"],"versions-pinned":["specified","planned","implemented","verified","accepted"],"ci-matrix":["specified","planned","implemented","verified","accepted"],"clean-clone-proof":["specified","planned","implemented","verified","accepted"],"developer-parity":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","build-manifest","ci-report","test-report"]
child-work-refs: []
budget-limits: null
---

# Build reproducible y CI obligatoria de producto completo (#223)

## Description

Convertir el repositorio en un build reproducible desde clone limpio para Java, UI, Workbench y Python, y proteger main con gates equivalentes. El enfoque incluye:
- Definir toolchains soportados y una orden bootstrap única
- Fijar Maven Wrapper y versiones de plugins (incluido JaCoCo)
- Resolver docs/node-idioms.md como artifact compartido o único por target plan
- Integrar npm ci o equivalente basado en lockfile antes de Vite
- Añadir jobs CI con caches sólo como optimización
- Ejecutar el mismo script localmente y en CI
- Hacer obligatorios los checks de main relevantes

## Acceptance criteria

- [ ] **single-bootstrap:** Un clone limpio dispone de una orden documentada que instala toolchains/dependencias bloqueadas por lockfile y ejecuta el build completo sin estado previo de node_modules.; stages: specified
- [ ] **maven-green:** El reactor Maven completo termina verde, incluyendo Node, CLI, API y renovatio-llm-evals.; stages: specified
- [ ] **node-collision:** La generación Node multiprograma no produce paths duplicados para docs/node-idioms.md y conserva determinismo del manifest.; stages: specified
- [ ] **versions-pinned:** Java, Maven plugins incluido JaCoCo, Node/package manager y Python están fijados o validados explícitamente.; stages: specified
- [ ] **ci-matrix:** CI ejecuta Maven, renovatio-ui, renovatio-workbench, Python, caracterización y equivalencia, con fallos de seguridad relevantes no tolerados mediante ignore incondicional.; stages: specified
- [ ] **clean-clone-proof:** La evidencia proviene de un clone temporal limpio sin targets ni node_modules heredados.; stages: specified
- [ ] **developer-parity:** Las mismas órdenes funcionan localmente y en CI y están documentadas en el README de desarrollo.; stages: specified

## Required artifacts

- spec
- implementation-plan
- build-manifest
- ci-report
- test-report
