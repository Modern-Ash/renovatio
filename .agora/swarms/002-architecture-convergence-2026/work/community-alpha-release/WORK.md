---
schema: "agora/work/v1"
id: "community-alpha-release"
swarm: "architecture-convergence-2026"
title: "AC-14: Publicar Renovatio 0.3.0-alpha como technical preview (#235)"
state: "implementing"
revision: 1
operational-status: "blocked"
status-reason: "Publication remains blocked: npm --prefix renovatio-workbench audit --audit-level=moderate fails on Theia 1.75.0 transitive vulnerabilities; Theia 1.75.0 is the latest published package checked on 2026-09-11, so Spec Owner must approve mitigation or wait for patched upstream before tag/release."
status-by: "project:agent"
status-at: "2026-09-11T20:14:26.391752Z"
acceptance-criteria: {"release-scope":"Release notes y capability matrix distinguen reference, beta, experimental y planned sin sobrepromesas.","quickstart":"Un contribuidor externo completa el quick start COBOL a Java offline en menos de diez minutos desde clone limpio.","docs":"README, arquitectura as-is/target, ADRs, seguridad, contribucion, troubleshooting y examples no contienen enlaces rotos ni estados historicos contradictorios.","versioning":"POMs, packages, UI, tags y artifacts usan una version coherente y existe changelog desde 0.2.0.","supply-chain":"Se publican checksums, SBOM, provenance de build y artifacts firmados segun la politica aprobada.","quality-gates":"Build completo, tests, caracterizacion, equivalencia, scans y smoke tests pasan sobre el commit exacto del tag.","clean-repository":"El tag no contiene DBs, targets, credenciales, PII ni evidencia interna no aprobada para distribucion.","approval":"El Spec Owner revisa el release-readiness report y aprueba explicitamente la publicacion; Agora conserva evidencia y handoff."}
satisfied-criteria: []
criterion-statuses: {"release-scope":["specified","planned"],"quickstart":["specified","planned"],"docs":["specified","planned"],"versioning":["specified","planned"],"supply-chain":["specified","planned"],"quality-gates":["specified","planned"],"clean-repository":["specified","planned"],"approval":["specified","planned"]}
required-artifacts: ["spec","implementation-plan","release-plan","community-docs","sbom","release-notes","release-readiness-report"]
child-work-refs: []
budget-limits: null
---

# AC-14: Publicar Renovatio 0.3.0-alpha como technical preview (#235)

## Description

Preparar una alpha comunitaria honesta y reproducible para Renovatio 0.3.0-alpha una vez cerradas las dependencias obligatorias de la epica #221. El trabajo cubre scope/capabilities, quick start, documentacion, versionado, supply chain, gates de calidad, limpieza del repositorio y aprobacion explicita del Spec Owner antes de cualquier tag o publicacion.

## Acceptance criteria

- [ ] **release-scope:** Release notes y capability matrix distinguen reference, beta, experimental y planned sin sobrepromesas.; stages: specified, planned
- [ ] **quickstart:** Un contribuidor externo completa el quick start COBOL a Java offline en menos de diez minutos desde clone limpio.; stages: specified, planned
- [ ] **docs:** README, arquitectura as-is/target, ADRs, seguridad, contribucion, troubleshooting y examples no contienen enlaces rotos ni estados historicos contradictorios.; stages: specified, planned
- [ ] **versioning:** POMs, packages, UI, tags y artifacts usan una version coherente y existe changelog desde 0.2.0.; stages: specified, planned
- [ ] **supply-chain:** Se publican checksums, SBOM, provenance de build y artifacts firmados segun la politica aprobada.; stages: specified, planned
- [ ] **quality-gates:** Build completo, tests, caracterizacion, equivalencia, scans y smoke tests pasan sobre el commit exacto del tag.; stages: specified, planned
- [ ] **clean-repository:** El tag no contiene DBs, targets, credenciales, PII ni evidencia interna no aprobada para distribucion.; stages: specified, planned
- [ ] **approval:** El Spec Owner revisa el release-readiness report y aprueba explicitamente la publicacion; Agora conserva evidencia y handoff.; stages: specified, planned

## Required artifacts

- spec
- implementation-plan
- release-plan
- community-docs
- sbom
- release-notes
- release-readiness-report
