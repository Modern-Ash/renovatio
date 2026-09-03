---
schema: "agora/work/v1"
id: "f7-renovatio-jcl"
swarm: "decision-engine-f7"
title: "F7 \u00b7 renovatio-jcl: batch orchestration (issue #153)"
state: "verifying"
revision: 2
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"jcl-parse":"JCL 3-step con COND parsea a BatchJob con conditionGraph correcto","cond-truth-table":"Semantica COND (skip step si condicion verdadera) cubierta por tabla de verdad + tests dedicados","spring-batch-emit":"Job de 3 steps encadenados emite spring-batch que ejecuta los 3 programas migrados en orden respetando la condicion","sort-fixture":"SORT FIELDS= + INCLUDE COND= produce salida identica a fixture de referencia","dd-datasets":"DD secuencial in/out -> archivos correctos; temp dataset no persiste","missing-proc":"Proc catalogada faltante -> manual action item, sin crash","characterization":"Job destino produce los mismos outputs de datos que el batch original para el fixture (harness #122)","defaults-safe":"Sin JCL en el proyecto -> comportamiento actual intacto"}
satisfied-criteria: []
criterion-statuses: {"jcl-parse":["specified","planned","implemented","verified"],"cond-truth-table":["specified","planned","implemented","verified"],"spring-batch-emit":["specified","planned","implemented","verified"],"sort-fixture":["specified","planned","implemented","verified"],"dd-datasets":["specified","planned","implemented","verified"],"missing-proc":["specified","planned","implemented","verified"],"characterization":["specified","planned","implemented","verified"],"defaults-safe":["specified","planned","implemented","verified"]}
required-artifacts: ["spec","implementation-plan","test-report"]
child-work-refs: []
budget-limits: null
---

# F7 · renovatio-jcl: batch orchestration (issue #153)

## Description

New renovatio-jcl module: JCL parser (JOB/EXEC PGM/EXEC PROC/DD/COND/IF-THEN-ELSE/SET, in-stream+catalogued procs); neutral semantic-IR model BatchJob{steps[],datasets[],conditionGraph}; per-step classification (migrated-program call / standard utility / residue); profile decision batch.target with spring-batch first; DD->resource mapping (sequential->file/stream, VSAM->F4 repo, temp->intermediate step); COND/IF->job control flow with explicit truth table; SORT->comparators+stream sort, IEBGENER/IDCAMS REPRO->copy, IDCAMS DELETE/DEFINE->resource mgmt; new BATCH LLM suggestion category for ambiguous steps; unknown utilities->manual action item. YAGNI: one target (spring-batch), common SORT/IDCAMS subset, no GDG/system catalog/mainframe scheduler/JCL-to-JCL. Depends on F2+F3.

## Acceptance criteria

- [ ] **jcl-parse:** JCL 3-step con COND parsea a BatchJob con conditionGraph correcto; stages: specified, planned, implemented, verified
- [ ] **cond-truth-table:** Semantica COND (skip step si condicion verdadera) cubierta por tabla de verdad + tests dedicados; stages: specified, planned, implemented, verified
- [ ] **spring-batch-emit:** Job de 3 steps encadenados emite spring-batch que ejecuta los 3 programas migrados en orden respetando la condicion; stages: specified, planned, implemented, verified
- [ ] **sort-fixture:** SORT FIELDS= + INCLUDE COND= produce salida identica a fixture de referencia; stages: specified, planned, implemented, verified
- [ ] **dd-datasets:** DD secuencial in/out -> archivos correctos; temp dataset no persiste; stages: specified, planned, implemented, verified
- [ ] **missing-proc:** Proc catalogada faltante -> manual action item, sin crash; stages: specified, planned, implemented, verified
- [ ] **characterization:** Job destino produce los mismos outputs de datos que el batch original para el fixture (harness #122); stages: specified, planned, implemented, verified
- [ ] **defaults-safe:** Sin JCL en el proyecto -> comportamiento actual intacto; stages: specified, planned, implemented, verified

## Required artifacts

- spec
- implementation-plan
- test-report
