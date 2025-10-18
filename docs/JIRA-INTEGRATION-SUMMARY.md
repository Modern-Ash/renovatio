# Resumen: Integración Jira en Spec-Driven Workflow

## 📋 Respuesta a tu pregunta

**Pregunta original**: 
> "Estoy tratando de usar @github/spec-kit/files/spec-driven.md en mi proyecto, y quiero saber si la parte de planning y task de la especificación puedo referenciar y usar ticket jira, ya el repo contiene la app de jira y github también"

**Respuesta**: ✅ **SÍ, absolutamente.**

El repositorio Renovatio ahora tiene soporte completo para referenciar y usar tickets de Jira en las secciones de planning y tasks de las especificaciones siguiendo el modelo spec-driven de @github/spec-kit.

---

## 🎯 Lo que se implementó

### 1. **Guía completa de integración**
📄 **[docs/JIRA-GITHUB-INTEGRATION.md](./JIRA-GITHUB-INTEGRATION.md)**

Esta guía incluye:
- ✅ Configuración inicial de Jira-GitHub
- ✅ Cómo usar Jira en especificaciones (metadatos YAML)
- ✅ Smart commits para automatización
- ✅ Workflows y best practices
- ✅ Ejemplos prácticos completos
- ✅ Troubleshooting

### 2. **Plantilla actualizada con soporte Jira**
📄 **[docs/specs/renovatio-spec-template.md](./specs/renovatio-spec-template.md)**

Cambios:
- ✅ Metadatos Jira en frontmatter (epic, story, tasks)
- ✅ Referencias a tickets en el resumen ejecutivo
- ✅ Tabla de tareas con links a Jira
- ✅ Checklist de sincronización Jira-GitHub

### 3. **Ejemplo completo con Jira**
📄 **[docs/specs/ejemplos/migracion-cobol-jira.md](./specs/ejemplos/migracion-cobol-jira.md)**

Un ejemplo real de especificación mostrando:
- ✅ Epic, Stories y Tasks en Jira
- ✅ Tabla de planificación con tickets
- ✅ Smart commits documentados
- ✅ Sincronización bidireccional
- ✅ Sprint planning integrado

### 4. **GitHub Actions para sincronización automática**
📄 **[.github/workflows/jira-sync.yml](../.github/workflows/jira-sync.yml)**

Workflow que automáticamente:
- ✅ Detecta tickets Jira en PRs/issues
- ✅ Añade comentarios en Jira cuando se abre/cierra PR
- ✅ Transiciona estados automáticamente (To Do → In Progress → Done)
- ✅ Valida archivos de especificación
- ✅ Añade links de Jira a las descripciones de PR

### 5. **Documentación actualizada**
Todos los documentos de Spec Kit fueron actualizados:
- ✅ [SPEC-KIT-QUICK-START.md](./SPEC-KIT-QUICK-START.md) - Ahora incluye ejemplos Jira
- ✅ [EXPLICACION-SPEC-KIT.md](./EXPLICACION-SPEC-KIT.md) - Nueva sección sobre Jira
- ✅ [specs/INDEX.md](./specs/INDEX.md) - Referencia a guía Jira
- ✅ [README.md](../README.md) - Link a guía de integración
- ✅ [ejemplos/README.md](./specs/ejemplos/README.md) - Nuevo ejemplo con Jira

---

## 🚀 Cómo usar Jira en tus especificaciones

### Paso 1: Añadir metadatos Jira en tu spec

```yaml
---
title: "Tu proyecto de migración"
status: "in-progress"

# Integración Jira (NUEVO)
jira_epic: "RENO-100"
jira_parent_story: "RENO-101"
linked_jira_issues: ["RENO-102", "RENO-103", "RENO-104"]
jira_project: "RENO"
jira_sprint: "Sprint 5"
jira_labels: ["migration", "cobol", "jpa"]
---
```

### Paso 2: Referenciar tickets en el contenido

```markdown
# Título del proyecto

> **Jira Epic**: [RENO-100](https://your-jira.atlassian.net/browse/RENO-100)  
> **GitHub Issue**: #150

## 4. Plan de ejecución

### Tareas planificadas (Jira Integration)

| Tarea | Jira Ticket | Asignado | Estado | Sprint |
|-------|-------------|----------|--------|--------|
| Análisis | [RENO-102](https://jira/RENO-102) | @dev1 | Done | Sprint 1 |
| Implementación | [RENO-103](https://jira/RENO-103) | @dev2 | In Progress | Sprint 1 |
```

### Paso 3: Usar smart commits

```bash
# Al hacer commits, incluye el ticket Jira
git commit -m "RENO-102 #time 2h #comment Implementado parser COBOL #close"

# Crea PRs con el ticket en el título
git push origin feature/RENO-102-parser
# PR Title: "RENO-102: Implementar parser COBOL"
```

### Paso 4: Deja que GitHub Actions haga el resto

El workflow automáticamente:
- Detecta el ticket en tu PR
- Añade comentarios en Jira
- Transiciona el estado
- Vincula el PR al ticket

---

## 📚 Recursos principales

| Recurso | Descripción |
|---------|-------------|
| **[JIRA-GITHUB-INTEGRATION.md](./JIRA-GITHUB-INTEGRATION.md)** | 📖 Guía completa (50+ páginas) |
| **[migracion-cobol-jira.md](./specs/ejemplos/migracion-cobol-jira.md)** | 🎯 Ejemplo real completo |
| **[renovatio-spec-template.md](./specs/renovatio-spec-template.md)** | 📝 Plantilla actualizada |
| **[jira-sync.yml](./.github/workflows/jira-sync.yml)** | 🤖 Workflow de automatización |

---

## ✅ Checklist para empezar

- [ ] Leer [JIRA-GITHUB-INTEGRATION.md](./JIRA-GITHUB-INTEGRATION.md)
- [ ] Verificar que la app GitHub-Jira esté instalada (Settings > Integrations)
- [ ] Configurar proyecto Jira (key sugerido: `RENO`)
- [ ] Copiar plantilla: `cp docs/specs/renovatio-spec-template.md docs/specs/activas/mi-proyecto.md`
- [ ] Añadir metadatos Jira a tu spec
- [ ] Crear Epic y Stories en Jira
- [ ] Referenciar tickets en la spec
- [ ] Hacer primer commit con smart commit: `RENO-XXX #start Iniciando trabajo`
- [ ] Crear PR con ticket en el título
- [ ] Ver la magia de la sincronización automática ✨

---

## 💡 Beneficios de la integración

### Para el equipo
- 📊 **Métricas unificadas**: Progreso visible en Jira y GitHub
- 🔄 **Sincronización automática**: Sin actualización manual de estados
- 📝 **Trazabilidad completa**: Desde planning hasta código
- 🎯 **Planning estructurado**: Specs vinculadas a épics/stories

### Para developers
- ⚡ **Smart commits**: Actualiza Jira desde Git
- 🔗 **Contexto completo**: Links directos entre spec, Jira y código
- 📋 **Claridad**: Tareas bien definidas en ambas plataformas
- 🤖 **Automatización**: Menos trabajo manual, más código

### Para managers
- 📈 **Visibilidad**: Estado del proyecto en tiempo real
- 🎯 **Burndown charts**: Métricas automáticas en Jira
- 📊 **Reportes**: Progreso por sprint, épica, módulo
- ✅ **Accountability**: Quién trabaja en qué, cuándo

---

## 🎓 Ejemplos de uso

### Ejemplo 1: Crear una nueva spec con Jira

```bash
# 1. Copiar plantilla
cp docs/specs/renovatio-spec-template.md \
   docs/specs/activas/migracion-customer.md

# 2. Editar y añadir metadatos Jira
vim docs/specs/activas/migracion-customer.md

# 3. Crear Epic en Jira
# https://your-jira.atlassian.net/browse/RENO-200

# 4. Crear Stories y Tasks

# 5. Actualizar spec con links a Jira

# 6. Commit
git add docs/specs/activas/migracion-customer.md
git commit -m "docs: Add migration spec for Customer module" -m "Related to RENO-200"
git push
```

### Ejemplo 2: Trabajar en una tarea Jira

```bash
# 1. Crear branch con ticket
git checkout -b feature/RENO-210-parser

# 2. Iniciar trabajo
git commit --allow-empty -m "RENO-210 #start Iniciando desarrollo del parser"

# 3. Trabajar y commitear con smart commits
git commit -m "RENO-210 #time 3h #comment Implementada lógica principal del parser"

# 4. Completar
git commit -m "RENO-210 #comment Parser completado, listo para revisión"
git push origin feature/RENO-210-parser

# 5. Crear PR con título: "RENO-210: Implementar parser COBOL"

# 6. Al mergear, cerrar ticket
git commit -m "RENO-210 #close Parser integrado exitosamente"
```

---

## 🎉 ¡Ya está todo listo!

Ahora puedes:
1. ✅ **Referenciar tickets Jira** en tus specs
2. ✅ **Usar planning y tasks** con Jira
3. ✅ **Automatizar** con smart commits
4. ✅ **Sincronizar** Jira ↔ GitHub automáticamente

**La respuesta a tu pregunta es: SÍ, puedes y debes usar Jira en tus especificaciones spec-driven.**

---

## 📞 Preguntas o problemas

- 📖 Lee la guía completa: [JIRA-GITHUB-INTEGRATION.md](./JIRA-GITHUB-INTEGRATION.md)
- 🎯 Revisa el ejemplo: [migracion-cobol-jira.md](./specs/ejemplos/migracion-cobol-jira.md)
- 🐛 Abre un issue en GitHub
- 💬 Consulta la sección de troubleshooting en la guía

---

**¡Buena suerte con tus migraciones usando Renovatio + Spec Kit + Jira!** 🚀
