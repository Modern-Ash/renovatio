# Especificaciones de Ejemplo - Renovatio con Spec Kit

Este directorio contiene especificaciones de ejemplo que demuestran cómo usar GitHub Spec Kit con Renovatio para documentar y ejecutar migraciones de código.

## 📚 Ejemplos Disponibles

### 1. [migracion-cobol-basica.md](./migracion-cobol-basica.md)
Ejemplo completo de migración de programa COBOL a Java con Spring Boot, sin complejidades adicionales (sin DB2, sin CICS).

**Casos de uso:**
- Primera migración COBOL → Java
- Programas con lógica procedural básica
- Archivos planos o datos simples

**Herramientas MCP utilizadas:**
- `cobol.analyze`
- `cobol.metrics`
- `cobol.plan`
- `cobol.apply`
- `cobol.diff`

**Complejidad**: Básica

---

### 2. [migracion-cobol-jira.md](./migracion-cobol-jira.md) 🆕
Ejemplo completo de migración COBOL con DB2 embebido a Java Spring Boot + JPA, **incluyendo integración completa con Jira**.

**Casos de uso:**
- Migración COBOL → Java con SQL embebido
- Programas con EXEC SQL (DB2)
- Planning y tracking con Jira
- Workflows enterprise con múltiples equipos
- Sincronización Jira-GitHub automatizada

**Herramientas MCP utilizadas:**
- `cobol.analyze`
- `cobol.metrics`
- `cobol.migrate_db2`
- `cobol.plan`
- `cobol.apply`

**Integración Jira:**
- Epic, Stories y Tasks en Jira
- Smart commits para automatización
- Tabla de tareas con links a Jira
- Sincronización bidireccional
- Sprint planning y tracking

**Complejidad**: Alta

**⭐ Recomendado para**: Proyectos que necesitan integrar Jira en el workflow de spec-driven development.

---

## 🎯 Cómo usar estos ejemplos

### Para comenzar una nueva migración

1. **Copia el ejemplo más cercano a tu caso de uso:**
   ```bash
   cp docs/specs/ejemplos/migracion-cobol-basica.md \
      docs/specs/activas/mi-migracion.md
   ```

2. **Personaliza los metadatos:**
   - Cambia `title`, `author`, `reviewers`
   - Actualiza `status` a `draft`
   - Ajusta `renovatio.tools_sequence` según tus necesidades
   - Define tus propias `expected_outcomes`

3. **Completa las secciones:**
   - Describe tu contexto específico
   - Define objetivos medibles
   - Identifica riesgos particulares
   - Planifica tus fases de ejecución

4. **Valida con Spec Kit:**
   ```bash
   spec-kit validate docs/specs/activas/mi-migracion.md
   ```

5. **Sincroniza con GitHub:**
   ```bash
   spec-kit sync --create-issues docs/specs/activas/mi-migracion.md
   ```

### Para ejecutar con Renovatio

Los metadatos `renovatio.tools_sequence` definen la secuencia de herramientas MCP a ejecutar:

```bash
# Ejemplo de ejecución manual
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "1",
    "method": "tools/call",
    "params": {
      "name": "cobol.analyze",
      "arguments": {
        "workspacePath": "/path/to/cobol"
      }
    }
  }'
```

O usando un script automatizado que lea la spec y ejecute las herramientas.

---

## 📖 Estructura de las especificaciones

Todas las especificaciones de ejemplo siguen esta estructura:

```yaml
---
# Metadatos Spec Kit
title: "..."
status: "ejemplo"
version: "1.0.0"
...

# Metadatos Renovatio
renovatio:
  source_language: "..."
  target_language: "..."
  tools_sequence: [...]
  expected_outcomes: {...}
---

# Título
## 1. Resumen ejecutivo
## 2. Objetivos y métricas
## 3. Alcance técnico
## 4. Diseño y plan de ejecución
## 5. Riesgos y consideraciones
## 6. Validación y pruebas
## 7. Checklist de implementación
## 8. Seguimiento y comunicación
## 9. Apéndice
```

---

## 🔗 Próximos ejemplos planificados

- **modernizacion-java17.md** - Upgrade de Java 8 a Java 17
- **batch-to-spring-batch.md** - JCL a Spring Batch
- **openrewrite-custom-recipe.md** - Crear receta OpenRewrite personalizada
- **cics-to-microservices.md** - CICS programs a microservicios Spring Boot

---

## 💡 Consejos

- **Empieza simple**: Usa ejemplos básicos para familiarizarte con el proceso
- **Itera**: Las specs son documentos vivos, actualízalas durante la ejecución
- **Comparte**: Tus specs exitosas pueden convertirse en ejemplos para otros
- **Automatiza**: Usa los metadatos `renovatio` para scripts de automatización

---

## 📚 Recursos adicionales

- **[JIRA-GITHUB-INTEGRATION.md](../../JIRA-GITHUB-INTEGRATION.md)** 🆕 - Guía completa de integración Jira
- [Guía de integración Spec Kit](../../spec-kit-integracion.md)
- [Explicación completa de Spec Kit](../../EXPLICACION-SPEC-KIT.md)
- [Plantilla base](../renovatio-spec-template.md)
- [README principal](../../../README.md)

---

**¿Tienes un caso de uso nuevo?** Crea tu spec y compártela como ejemplo para la comunidad.
