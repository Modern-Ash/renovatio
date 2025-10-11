# Título del proyecto / iniciativa

## 1. Resumen ejecutivo
- **Contexto**: ¿Qué problema del cliente o del producto aborda este trabajo?
- **Estado actual**: breve descripción de la situación actual en Renovatio.
- **Resultado esperado**: visión de éxito (qué cambiará cuando el trabajo finalice).

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
1. Paso 1 — descripción, entradas, salidas, responsables.
2. Paso 2 — …
3. Paso 3 — …

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
- [ ] Issues creados / enlazados.
- [ ] PRs asociadas a cada módulo.
- [ ] Documentación actualizada.
- [ ] Pipelines / MCP tools configurados.
- [ ] Validaciones ejecutadas y documentadas.

## 8. Seguimiento y comunicación
- Stakeholders clave.
- Plan de comunicación (demos, reportes, sincronizaciones).
- Próximos pasos después del cierre.

## 9. Apéndice
- Evidencia adicional, enlaces, diagramas, resultados de pruebas, etc.

---

_Esta plantilla está adaptada para Renovatio y se puede usar con `spec-kit init --template docs/specs/renovatio-spec-template.md`._
