---
schema: "agora/work/v1"
id: "domain-to-architecture-projection"
swarm: "architecture-projection-rework"
title: "Proyectar DomainModel a arquitecturas destino"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"architecture-contract":"ArchitectureModel versionado representa m\u00f3dulos, componentes, ports, adapters, controllers, services, repositories y relaciones derivados del DomainModel.","transaction-script":"Transaction Script ofrece una proyecci\u00f3n m\u00ednima y compatible por caso de uso/programa.","layered-mvc":"Layered MVC cuenta con transformaci\u00f3n y layout reales; no se anuncia como activo sin implementaci\u00f3n.","hexagonal":"Hexagonal crea ports/adapters s\u00f3lo cuando existen boundaries sustentadas y registra fallbacks por programa.","target-compatibility":"ArchitectureModel puede alimentar TargetModel/emitters sin reinterpretar COBOL y mantiene determinismo.","regression-quality":"Tests focalizados, compilaci\u00f3n Maven y diff-check pasan sin romper el flujo actual."}
satisfied-criteria: ["architecture-contract","transaction-script","layered-mvc","hexagonal","target-compatibility","regression-quality"]
criterion-statuses: {"architecture-contract":["specified","planned","implemented","verified","accepted"],"transaction-script":["specified","planned","implemented","verified","accepted"],"layered-mvc":["specified","planned","implemented","verified","accepted"],"hexagonal":["specified","planned","implemented","verified","accepted"],"target-compatibility":["specified","planned","implemented","verified","accepted"],"regression-quality":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Proyectar DomainModel a arquitecturas destino

## Description

Convertir el DomainModel confirmado en un ArchitectureModel determinista y compatible con los emitters existentes.

## Acceptance criteria

- [x] **architecture-contract:** ArchitectureModel versionado representa módulos, componentes, ports, adapters, controllers, services, repositories y relaciones derivados del DomainModel.; stages: specified, planned, implemented, verified, accepted
- [x] **transaction-script:** Transaction Script ofrece una proyección mínima y compatible por caso de uso/programa.; stages: specified, planned, implemented, verified, accepted
- [x] **layered-mvc:** Layered MVC cuenta con transformación y layout reales; no se anuncia como activo sin implementación.; stages: specified, planned, implemented, verified, accepted
- [x] **hexagonal:** Hexagonal crea ports/adapters sólo cuando existen boundaries sustentadas y registra fallbacks por programa.; stages: specified, planned, implemented, verified, accepted
- [x] **target-compatibility:** ArchitectureModel puede alimentar TargetModel/emitters sin reinterpretar COBOL y mantiene determinismo.; stages: specified, planned, implemented, verified, accepted
- [x] **regression-quality:** Tests focalizados, compilación Maven y diff-check pasan sin romper el flujo actual.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- verification-report
- review-report
