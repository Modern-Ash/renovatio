---
schema: "agora/work/v1"
id: "epic-llm-cobol-discovery"
swarm: "llm-cobol-discovery-epic"
title: "Epic: reducir heur\u00edsticas hardcodeadas en el discovery de COBOL v\u00eda LLM gobernado"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"cics-verb-done":"Clasificaci\u00f3n de verbos CICS resuelta v\u00eda LLM gobernado o decisi\u00f3n documentada de no hacerlo","control-break-done":"Detecci\u00f3n de control-break resuelta v\u00eda LLM gobernado o decisi\u00f3n documentada de no hacerlo","dynamic-filename-done":"Resoluci\u00f3n de nombre de archivo din\u00e1mico CICS resuelta o decisi\u00f3n documentada","residual-routing-done":"Routing de construcciones no clasificadas a discovery LLM implementado","copy-replacing-done":"Soporte de COPY REPLACING resuelto v\u00eda LLM o decisi\u00f3n documentada de no hacerlo"}
satisfied-criteria: []
criterion-statuses: {"cics-verb-done":[],"control-break-done":[],"dynamic-filename-done":[],"residual-routing-done":[],"copy-replacing-done":[]}
required-artifacts: []
child-work-refs: ["llm-cobol-discovery-epic/cics-verb-classification-llm","llm-cobol-discovery-epic/control-break-detection-llm-eval","llm-cobol-discovery-epic/cics-dynamic-filename-resolution","llm-cobol-discovery-epic/residual-discovery-llm-routing","llm-cobol-discovery-epic/copy-replacing-support-eval","llm-cobol-discovery-epic/ollama-cobol-model-benchmark","llm-cobol-discovery-epic/xmainframe-summarization-benchmark"]
budget-limits: null
---

# Epic: reducir heurísticas hardcodeadas en el discovery de COBOL vía LLM gobernado

## Description

## Objetivo

Extender el patrón validado por el epic `epic-llm-domain-entities` (prompt gobernado + hechos deterministas de grounding + validador anti-alucinación + fallback determinista) a otros cinco puntos del *discovery* de COBOL (la fase de análisis/parsing, no la de generación de código destino) donde hoy Renovatio usa listas hardcodeadas o regex frágiles para "reconocer una forma" en el código fuente, en vez de aprovechar la infraestructura LLM gobernada que ya existe (`renovatio-llm`, `renovatio-llm-runtime`).

Este epic nace de la misma sesión de trabajo que produjo `epic-llm-domain-entities`: al mapear la complejidad de los 26 módulos de Renovatio se identificaron varios puntos adicionales, más chicos y acotados, que son candidatos igual de claros para el mismo patrón — no se investigaron con el mismo detalle que la inferencia de entidades/relaciones (que sí se prototipó), así que cada issue hija incluye explícitamente una tarea de "confirmar el diagnóstico" antes de implementar, para no asumir de más.

## Acceptance criteria

- [ ] **cics-verb-done:** Clasificación de verbos CICS resuelta vía LLM gobernado o decisión documentada de no hacerlo; stages: none
- [ ] **control-break-done:** Detección de control-break resuelta vía LLM gobernado o decisión documentada de no hacerlo; stages: none
- [ ] **dynamic-filename-done:** Resolución de nombre de archivo dinámico CICS resuelta o decisión documentada; stages: none
- [ ] **residual-routing-done:** Routing de construcciones no clasificadas a discovery LLM implementado; stages: none
- [ ] **copy-replacing-done:** Soporte de COPY REPLACING resuelto vía LLM o decisión documentada de no hacerlo; stages: none

## Required artifacts

- none
