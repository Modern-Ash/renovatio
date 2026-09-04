---
schema: "agora/tool-result/v1"
run: "tool-20260904t14401788543640z"
status: "completed"
exit-code: 0
result-kind: "work-item"
---

# Tool result tool-20260904t14401788543640z

## Standard output

    {"assignees":[],"body":"## Contexto\n\nParte del epic \"motor de migración parametrizable por decisiones\". Depende de F3 y F4.\n\nValidación de que la arquitectura es multi-target de verdad: agregar un segundo lenguaje destino debe ser \"solo\" escribir un emitter + un catálogo de patrones idiomáticos, **sin tocar el análisis COBOL**.\n\n## Alcance\n\n### `NodeEmitter` (o `PythonEmitter`) `implements TargetEmitter`\n- Módulo `renovatio-emitter-node` (o `-python`).\n- Node: generación vía plantillas / `ts-morph`; target `NestJS` o `Express` según `profile.runtime.framework`. Python: `libcst` / plantillas; target `FastAPI`.\n- Consume el mismo `TargetModel` que produce `renovatio-architecture` (F3) — sin ramas COBOL-específicas.\n\n### Catálogo de patrones idiomáticos del lenguaje nuevo\n- Mapeos \"intent semántico → construcción idiomática\" para el lenguaje (equivalente a lo que hace hoy `cobol-openrewrite-recipes` para Java).\n- Se puede **generar con LLM offline y revisar** (no en el hot path de migración).\n\n### `PersistenceStrategy` para el lenguaje nuevo\n- Al menos una estrategia (ej. Node: Prisma o TypeORM; Python: SQLAlchemy).\n\n### UI\n- Habilitar `NODE` (o `PYTHON`) en el select de lenguaje del Step \"Target\".\n\n## Riesgos\n1. **Fugas Java descubiertas tarde** en `TargetModel` o `semantic-IR`. Mitigación: si aparecen, son bugs de F2/F3 — se corrigen ahí, no se parchean en el emitter.\n2. **Toolchain del lenguaje nuevo** (node/npm o python en la máquina del usuario). Mitigación: el emitter genera el proyecto pero no exige ejecutarlo; la compilación/verificación del target es opcional.\n\n## YAGNI\n- Un solo lenguaje nuevo, no dos.\n- Un framework por lenguaje, no varios.\n- Sin paridad total de patrones — los faltantes salen como manual action items.\n\n## Verificación\n1. Un fixture COBOL migrado a Java (F3) y al lenguaje nuevo → ambos proyectos tienen la misma estructura de casos de uso / puertos.\n2. El proyecto generado del lenguaje nuevo levanta (endpoint responde) con un fixture chico.\n3. Ningún cambio en `renovatio-cobol-ir` / `renovatio-semantic-ir` / `renovatio-provider-cobol` en el diff de esta fase (salvo bugfixes de fuga documentados).\n\n## Gobernanza\nBajo Agora, spec-driven + TDD, un ciclo spec→plan→implementación.\n\n---\n🤖 Generated with [Claude Code](https://claude.com/claude-code)\n","createdAt":"2026-08-31T22:45:55Z","labels":[{"id":"LA_kwDOPowW0s8AAAACJUCmtw","name":"enhancement","description":"New feature or request","color":"a2eeef"},{"id":"LA_kwDOPowW0s8AAAACJUP9zg","name":"codex","description":"","color":"ededed"}],"milestone":null,"number":150,"state":"CLOSED","stateReason":"COMPLETED","title":"F5 · Segundo lenguaje destino: NodeEmitter (o Python) + catálogo de patrones idiomáticos","updatedAt":"2026-09-02T01:39:51Z","url":"https://github.com/Modern-Ash/renovatio/issues/150"}

## Standard error

    (empty)
