# Título del proyecto / iniciativa

> **Jira Tracking**: [PROJ-123](https://your-jira-instance.atlassian.net/browse/PROJ-123)  
> **GitHub Issue**: #123

## 1. Resumen ejecutivo
- **Contexto**: ¿Qué problema del cliente o del producto aborda este trabajo?
- **Estado actual**: breve descripción de la situación actual en Renovatio.
- **Resultado esperado**: visión de éxito (qué cambiará cuando el trabajo finalice).
- **Jira Epic/Story**: Enlace a la épica o historia principal en Jira (si aplica)

## 2. Objetivos y métricas
- Objetivo 1 — _ejemplo_: Permitir migrar copybooks con DB2 embebido a entidades JPA automáticamente.
- Objetivo 2 — _ejemplo_: Reducir el tiempo de ejecución de `tools/apply` en un 20%.
- Métricas clave: describe KPIs o métricas de validación.

## 3. Alcance técnico
### Módulos involucrados
- `renovatio-core`: …
- `renovatio-provider-java`: …
- `renovatio-provider-cobol`: …
- `renovatio-mcp-server`: …
- `renovatio-shared`: …
- Documentación (`docs/`, `examples/`, otros): …

### APIs MCP / herramientas
- `tools/list`: ajustes previstos.
- `tools/describe`: nuevos esquemas o propiedades.
- `tools/call`: secuencia de herramientas que se ejecutarán (incluye parámetros relevantes).

### Artefactos adicionales
- Recetas OpenRewrite requeridas.
- Plantillas Freemarker / generadores.
- Configuraciones (`rewrite.yml`, `application.yml`).

## 4. Diseño y plan de ejecución

### Tareas planificadas (Jira Integration)
| Tarea | Jira Ticket | Asignado | Estado | Estimación |
|-------|-------------|----------|--------|------------|
| Paso 1: Análisis inicial | [PROJ-124](https://your-jira.atlassian.net/browse/PROJ-124) | @usuario1 | To Do | 2d |
| Paso 2: Implementación | [PROJ-125](https://your-jira.atlassian.net/browse/PROJ-125) | @usuario2 | In Progress | 5d |
| Paso 3: Testing | [PROJ-126](https://your-jira.atlassian.net/browse/PROJ-126) | @usuario3 | To Do | 3d |

### Detalles del plan
1. **Paso 1** — descripción, entradas, salidas, responsables.
   - Jira: [PROJ-124](https://your-jira.atlassian.net/browse/PROJ-124)
   - GitHub PR: (to be created)
2. **Paso 2** — descripción, entradas, salidas, responsables.
   - Jira: [PROJ-125](https://your-jira.atlassian.net/browse/PROJ-125)
   - GitHub PR: (to be created)
3. **Paso 3** — descripción, entradas, salidas, responsables.
   - Jira: [PROJ-126](https://your-jira.atlassian.net/browse/PROJ-126)
   - GitHub PR: (to be created)

> Incluye diagramas, pseudocódigo o referencias a archivos específicos cuando sea necesario.

## 5. Riesgos y consideraciones
- Riesgo 1 — impacto, plan de mitigación.
- Riesgo 2 — …
- Dependencias externas (equipo, librerías, aprobaciones).

## 6. Validación y pruebas
- **Automatizadas**: comandos (`mvn test`, `npm test`, `./gradlew`, etc.), datasets, coverage.
- **Manual / Exploratoria**: pasos concretos, criterios de aceptación.
- **Monitoreo**: métricas a revisar después del despliegue.

## 7. Checklist de implementación

### Planning & Tracking
- [ ] Jira epic/story creada: [PROJ-123](https://your-jira.atlassian.net/browse/PROJ-123)
- [ ] Sub-tareas Jira creadas y enlazadas
- [ ] GitHub issues creados y sincronizados con Jira
- [ ] Sprint asignado en Jira

### Implementación
- [ ] PRs asociadas a cada módulo (con referencia Jira en título/descripción)
- [ ] Documentación actualizada
- [ ] Pipelines / MCP tools configurados
- [ ] Validaciones ejecutadas y documentadas

### Sincronización Jira-GitHub
- [ ] Smart commits configurados (PROJ-XXX #comment, #time, etc.)
- [ ] GitHub-Jira app integrada y funcionando
- [ ] Estados sincronizados automáticamente

## 8. Seguimiento y comunicación
- Stakeholders clave.
- Plan de comunicación (demos, reportes, sincronizaciones).
- Próximos pasos después del cierre.

## 9. Apéndice
- Evidencia adicional, enlaces, diagramas, resultados de pruebas, etc.

---

_Esta plantilla está adaptada para Renovatio y se puede usar con `spec-kit init --template docs/specs/renovatio-spec-template.md`._
