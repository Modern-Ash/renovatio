---
schema: "agora/tool-result/v1"
run: "tool-20260901t17531788296021z"
status: "completed"
exit-code: 0
result-kind: "work-item"
---

# Tool result tool-20260901t17531788296021z

## Standard output

    {"assignees":[],"body":"## Contexto\n\nParte del epic \"motor de migración parametrizable por decisiones\". Depende de F2 (semantic-IR + `TargetEmitter`).\n\n\"Hexagonal\", \"MVC\", \"transaction-script\" **no son templates** — son reorganizaciones del modelo semántico (puertos, adaptadores, casos de uso, entidades) que ocurren **antes** de emitir, y valen para los 3 lenguajes.\n\n## Alcance\n\n### `renovatio-architecture` (módulo nuevo)\n- `ArchitectureProfile`: función `SemanticIR → TargetModel` parametrizada por `profile.architecture.style`.\n- Dos estilos en esta fase (YAGNI):\n  - `TRANSACTION_SCRIPT` — el más cercano a COBOL, mínima impedancia: un servicio por programa, métodos por paragraph estructurado.\n  - `HEXAGONAL` — detección de casos de uso, puertos de entrada/salida, adaptadores; entidades separadas de la lógica.\n- `moduleGrouping` (de F1): agrupar programas COBOL en módulos / bounded contexts (por copybook de dominio, por prefijo, manual).\n- Detección de casos de uso y de agrupación puede pedir sugerencia LLM (reusa `DecisionSuggestionService` de F1, categoría `ARCHITECTURE`).\n\n### Web app\n- Step \"Target\" (de F1): el preview del layout de carpetas ahora es **real** — refleja el `TargetModel` resultante antes de generar.\n- Vista de arquitectura: diagrama de módulos/puertos/adaptadores generado desde el IR (no dibujado a mano).\n\n## Riesgos\n1. **Structuring pass de control flow** (GO TO → estructurado) es la parte dura; puede necesitar plan LLM + `ControlFlowPlanGate` (ya existe en `renovatio-llm`). Mitigación: si un programa no se puede estructurar con confianza, cae a `TRANSACTION_SCRIPT` con métodos privados + comentario, nunca se inventa.\n2. **Explosión de combinaciones** estilo × lenguaje. Mitigación: `ArchitectureProfile` produce `TargetModel` neutral; el emitter no sabe de estilos.\n\n## YAGNI\n- Solo 2 estilos, no 6 (clean/onion/CQRS quedan fuera).\n- Sin generación de tests del código destino.\n- Sin refactor de los emitters (F2 ya los dejó pluggables).\n\n## Verificación\n1. Mismo COBOL fixture → `TRANSACTION_SCRIPT` y `HEXAGONAL` producen dos layouts Java distintos y compilables.\n2. `moduleGrouping` por copybook agrupa correctamente los fixtures multi-programa.\n3. El preview del layout en la UI coincide con los archivos realmente emitidos.\n4. Caracterización: `style = TRANSACTION_SCRIPT` + profile defaults → salida equivalente a la de hoy (o diff documentado y aceptado).\n\n## Gobernanza\nBajo Agora, spec-driven + TDD, un ciclo spec→plan→implementación.\n\n---\n🤖 Generated with [Claude Code](https://claude.com/claude-code)\n","createdAt":"2026-08-31T22:45:52Z","labels":[{"id":"LA_kwDOPowW0s8AAAACJUCmtw","name":"enhancement","description":"New feature or request","color":"a2eeef"},{"id":"LA_kwDOPowW0s8AAAACJUP9zg","name":"codex","description":"","color":"ededed"}],"milestone":null,"number":148,"state":"OPEN","stateReason":"","title":"F3 · Arquitectura como transformación IR→IR: renovatio-architecture (transaction-script + hexagonal)","updatedAt":"2026-08-31T22:46:37Z","url":"https://github.com/Modern-Ash/renovatio/issues/148"}

## Standard error

    (empty)
