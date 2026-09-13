---
schema: "agora/work/v1"
id: "community-alpha-release"
swarm: "architecture-convergence-2026"
title: "AC-14: Publicar Renovatio 0.3.0-alpha como technical preview (#235)"
state: "completed"
revision: 1
operational-status: "active"
status-reason: "PR #260 was merged on 2026-09-11 at ec59a7e2 after review comments and CI passed; GitHub issue #235 is closed, so the stale pre-merge publication blocker is cleared for Agora closure."
status-by: "project:agent"
status-at: "2026-09-11T21:26:07.650460Z"
acceptance-criteria: {"release-scope":"Release notes y capability matrix distinguen reference, beta, experimental y planned sin sobrepromesas.","quickstart":"Un contribuidor externo completa el quick start COBOL a Java offline en menos de diez minutos desde clone limpio.","docs":"README, arquitectura as-is/target, ADRs, seguridad, contribucion, troubleshooting y examples no contienen enlaces rotos ni estados historicos contradictorios.","versioning":"POMs, packages, UI, tags y artifacts usan una version coherente y existe changelog desde 0.2.0.","supply-chain":"Se publican checksums, SBOM, provenance de build y artifacts firmados segun la politica aprobada.","quality-gates":"Build completo, tests, caracterizacion, equivalencia, scans y smoke tests pasan sobre el commit exacto del tag.","clean-repository":"El tag no contiene DBs, targets, credenciales, PII ni evidencia interna no aprobada para distribucion.","approval":"El Spec Owner revisa el release-readiness report y aprueba explicitamente la publicacion; Agora conserva evidencia y handoff."}
satisfied-criteria: ["release-scope","quickstart","docs","versioning","supply-chain","quality-gates","clean-repository","approval"]
criterion-statuses: {"release-scope":["specified","planned","implemented","verified","accepted"],"quickstart":["specified","planned","implemented","verified","accepted"],"docs":["specified","planned","implemented","verified","accepted"],"versioning":["specified","planned","implemented","verified","accepted"],"supply-chain":["specified","planned","implemented","verified","accepted"],"quality-gates":["specified","planned","implemented","verified","accepted"],"clean-repository":["specified","planned","implemented","verified","accepted"],"approval":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","release-plan","community-docs","sbom","release-notes","release-readiness-report"]
child-work-refs: []
budget-limits: null
---

# AC-14: Publicar Renovatio 0.3.0-alpha como technical preview (#235)

## Description

Preparar una alpha comunitaria honesta y reproducible para Renovatio 0.3.0-alpha una vez cerradas las dependencias obligatorias de la epica #221. El trabajo cubre scope/capabilities, quick start, documentacion, versionado, supply chain, gates de calidad, limpieza del repositorio y aprobacion explicita del Spec Owner antes de cualquier tag o publicacion.

## Acceptance criteria

- [x] **release-scope:** Release notes y capability matrix distinguen reference, beta, experimental y planned sin sobrepromesas.; stages: specified, planned, implemented, verified, accepted
- [x] **quickstart:** Un contribuidor externo completa el quick start COBOL a Java offline en menos de diez minutos desde clone limpio.; stages: specified, planned, implemented, verified, accepted
- [x] **docs:** README, arquitectura as-is/target, ADRs, seguridad, contribucion, troubleshooting y examples no contienen enlaces rotos ni estados historicos contradictorios.; stages: specified, planned, implemented, verified, accepted
- [x] **versioning:** POMs, packages, UI, tags y artifacts usan una version coherente y existe changelog desde 0.2.0.; stages: specified, planned, implemented, verified, accepted
- [x] **supply-chain:** Se publican checksums, SBOM, provenance de build y artifacts firmados segun la politica aprobada.; stages: specified, planned, implemented, verified, accepted
- [x] **quality-gates:** Build completo, tests, caracterizacion, equivalencia, scans y smoke tests pasan sobre el commit exacto del tag.; stages: specified, planned, implemented, verified, accepted
- [x] **clean-repository:** El tag no contiene DBs, targets, credenciales, PII ni evidencia interna no aprobada para distribucion.; stages: specified, planned, implemented, verified, accepted
- [x] **approval:** El Spec Owner revisa el release-readiness report y aprueba explicitamente la publicacion; Agora conserva evidencia y handoff.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- release-plan
- community-docs
- sbom
- release-notes
- release-readiness-report
