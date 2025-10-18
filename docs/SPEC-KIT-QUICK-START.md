# Spec Kit Quick Start - Guía Rápida

> **Inicio rápido para usar GitHub Spec Kit con Renovatio**

## ⚡ En 5 minutos

### 1. Instalar Spec Kit

```bash
# Opción 1: Global con npm
npm install -g @github/spec-kit

# Opción 2: En el proyecto
npm install --save-dev @github/spec-kit

# Verificar instalación
spec-kit --version
```

### 2. Crear tu primera especificación

```bash
# Desde un ejemplo
cp docs/specs/ejemplos/migracion-cobol-basica.md \
   docs/specs/activas/mi-proyecto.md

# O desde la plantilla
spec-kit init --template docs/specs/renovatio-spec-template.md \
              --output docs/specs/activas/mi-proyecto.md
```

### 3. Personalizar metadatos

```yaml
---
title: "Tu proyecto de migración"
status: "draft"
author: "tu-nombre"
renovatio:
  source_language: "cobol"  # o "java"
  target_language: "java"
  tools_sequence:
    - name: "cobol.analyze"
      params:
        workspacePath: "/path/to/workspace"
---
```

### 4. Validar y sincronizar

```bash
# Validar formato
spec-kit validate docs/specs/activas/mi-proyecto.md

# Crear issues en GitHub
spec-kit sync --create-issues docs/specs/activas/mi-proyecto.md
```

### 5. Ejecutar con Renovatio

```bash
# Levantar servidor MCP
java -jar renovatio-mcp-server/target/renovatio-mcp-server-*.jar

# Ejecutar herramientas según la spec
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "cobol.analyze",
      "arguments": {"workspacePath": "/path/to/workspace"}
    }
  }'
```

---

## 📖 Flujo de trabajo completo

```mermaid
graph LR
    A[Crear Spec] --> B[Validar]
    B --> C[Revisar]
    C --> D[Aprobar]
    D --> E[Ejecutar con Renovatio]
    E --> F[Validar resultados]
    F --> G[Actualizar Spec]
    G --> H[Cerrar]
```

---

## 📋 Comandos esenciales

### Gestión de specs

```bash
# Crear nueva spec
spec-kit init --template <plantilla> --output <archivo>

# Validar spec
spec-kit validate <archivo.md>

# Actualizar estado
spec-kit update --status in-progress <archivo.md>

# Listar todas las specs
spec-kit list docs/specs/activas/

# Generar reporte
spec-kit report --format markdown
```

### Sincronización con GitHub

```bash
# Crear issues desde spec
spec-kit sync --create-issues <archivo.md>

# Actualizar issues existentes
spec-kit sync --update <archivo.md>

# Cerrar issues completados
spec-kit sync --close <archivo.md>
```

### Con variables de entorno

```bash
# Configurar token de GitHub
export GITHUB_TOKEN="ghp_xxxxxxxxxxxx"

# Sincronizar
spec-kit sync --create-issues docs/specs/activas/*.md
```

---

## 🎯 Estados de una spec

| Estado | Descripción | Siguiente paso |
|--------|-------------|----------------|
| `draft` | Borrador inicial | Completar y revisar |
| `in-review` | En revisión técnica | Aprobar o solicitar cambios |
| `approved` | Aprobada para ejecución | Ejecutar con Renovatio |
| `in-progress` | En ejecución | Monitorear progreso |
| `completed` | Finalizada y validada | Archivar y documentar |
| `on-hold` | En pausa temporalmente | Reactivar cuando sea posible |
| `cancelled` | Cancelada | Documentar razones |

---

## 🔧 Integración con Renovatio MCP

### Metadatos `renovatio` en la spec

```yaml
renovatio:
  source_language: "cobol"
  target_language: "java"
  complexity: "básica" | "media" | "alta"
  estimated_duration: "1-2 semanas"
  modules:
    - "renovatio-provider-cobol"
  tools_sequence:
    - name: "cobol.analyze"
      params:
        workspacePath: "${WORKSPACE}"
    - name: "cobol.plan"
      params:
        workspacePath: "${WORKSPACE}"
        targetLanguage: "java"
  expected_outcomes:
    files_migrated: 15
    success_rate: ">= 95%"

# Integración Jira (NUEVO)
jira_epic: "RENO-100"
jira_parent_story: "RENO-101"
linked_jira_issues: ["RENO-102", "RENO-103"]
jira_project: "RENO"
jira_sprint: "Sprint 1"
```

> **💡 Tip**: Consulta [JIRA-GITHUB-INTEGRATION.md](./JIRA-GITHUB-INTEGRATION.md) para aprender a usar Jira tickets en tus specs.

### Ejecutar secuencia de herramientas

Puedes crear un script que lea la spec y ejecute las herramientas automáticamente:

```bash
#!/bin/bash
# execute-spec.sh

SPEC_FILE=$1
MCP_SERVER="http://localhost:8081/mcp"

# Extraer tools_sequence de la spec (requiere jq o yq)
# Para cada herramienta:
#   - Leer name y params
#   - Ejecutar via MCP
#   - Guardar resultados
#   - Actualizar spec con progreso
```

---

## 📚 Plantillas disponibles

### Para copiar y personalizar

```bash
# Migración COBOL básica
cp docs/specs/ejemplos/migracion-cobol-basica.md \
   docs/specs/activas/mi-migracion.md

# Plantilla genérica
cp docs/specs/renovatio-spec-template.md \
   docs/specs/activas/nueva-iniciativa.md
```

### Estructura recomendada

```
docs/specs/
├── renovatio-spec-template.md    # Plantilla base
├── ejemplos/                      # Especificaciones de ejemplo
│   ├── migracion-cobol-basica.md
│   └── README.md
├── activas/                       # Specs en progreso
│   └── ...
└── completadas/                   # Specs finalizadas
    └── ...
```

---

## 🚀 Ejemplo completo

### 1. Crear spec para migrar COBOL

```bash
cp docs/specs/ejemplos/migracion-cobol-basica.md \
   docs/specs/activas/migracion-customer.md
```

### 2. Personalizar

```yaml
---
title: "Migración módulo Customer COBOL a Java"
status: "draft"
author: "juan-dev"
renovatio:
  tools_sequence:
    - name: "cobol.analyze"
      params:
        workspacePath: "/workspace/cobol/customer"
---
```

### 3. Validar

```bash
spec-kit validate docs/specs/activas/migracion-customer.md
# ✅ Especificación válida
```

### 4. Crear issues

```bash
export GITHUB_TOKEN="ghp_xxxx"
spec-kit sync --create-issues docs/specs/activas/migracion-customer.md
# ✅ Creados 5 issues en GitHub Projects
```

### 5. Aprobar

```bash
spec-kit update --status approved docs/specs/activas/migracion-customer.md
```

### 6. Ejecutar con Renovatio

```bash
# Análisis
curl -X POST http://localhost:8081/mcp -d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "cobol.analyze",
    "arguments": {"workspacePath": "/workspace/cobol/customer"}
  }
}'

# Plan
curl -X POST http://localhost:8081/mcp -d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "cobol.plan",
    "arguments": {
      "workspacePath": "/workspace/cobol/customer",
      "targetLanguage": "java"
    }
  }
}'

# Aplicar
curl -X POST http://localhost:8081/mcp -d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "cobol.apply",
    "arguments": {
      "planId": "plan-12345",
      "dryRun": false
    }
  }
}'
```

### 7. Actualizar progreso

```bash
spec-kit update --status in-progress docs/specs/activas/migracion-customer.md
```

### 8. Completar

```bash
spec-kit update --status completed docs/specs/activas/migracion-customer.md

# Mover a completadas
mv docs/specs/activas/migracion-customer.md \
   docs/specs/completadas/
```

---

## 🔍 Troubleshooting

### Spec Kit no encontrado

```bash
# Verificar instalación
which spec-kit
npm list -g @github/spec-kit

# Reinstalar si es necesario
npm install -g @github/spec-kit
```

### Error de validación

```bash
# Ver detalles del error
spec-kit validate --verbose <archivo.md>

# Verificar formato YAML en metadatos
# Los metadatos deben estar entre --- y ---
```

### Sincronización con GitHub falla

```bash
# Verificar token
echo $GITHUB_TOKEN

# Permisos necesarios: repo, project
# Generar token en: https://github.com/settings/tokens

# Verificar conectividad
curl -H "Authorization: token $GITHUB_TOKEN" \
     https://api.github.com/user
```

---

## 📖 Más información

- **[EXPLICACION-SPEC-KIT.md](./EXPLICACION-SPEC-KIT.md)** - Explicación completa y propuestas de mejora
- **[spec-kit-integracion.md](./spec-kit-integracion.md)** - Guía detallada de integración
- **[JIRA-GITHUB-INTEGRATION.md](./JIRA-GITHUB-INTEGRATION.md)** - 🆕 Integración Jira-GitHub en spec-driven workflow
- **[specs/ejemplos/](./specs/ejemplos/)** - Especificaciones de ejemplo
- **[GitHub Spec Kit oficial](https://github.com/github/spec-kit)** - Repositorio y documentación

---

## ✅ Checklist de adopción

- [ ] Instalar Spec Kit
- [ ] Crear primera spec desde ejemplo
- [ ] Validar formato
- [ ] Configurar GITHUB_TOKEN
- [ ] Crear issues de prueba
- [ ] Ejecutar herramientas Renovatio
- [ ] Documentar resultados
- [ ] Compartir con el equipo

---

**¿Listo para empezar?** Copia un ejemplo de `docs/specs/ejemplos/` y personalízalo para tu proyecto.
