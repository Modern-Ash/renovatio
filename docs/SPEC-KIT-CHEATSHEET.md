# Spec Kit - Hoja de Referencia Rápida

> **Comandos y referencias esenciales para usar GitHub Spec Kit con Renovatio**

---

## 🚀 Comandos Básicos

### Instalación
```bash
npm install -g @github/spec-kit
spec-kit --version
```

### Crear Especificación
```bash
# Desde plantilla
cp docs/specs/ejemplos/migracion-cobol-basica.md docs/specs/activas/mi-spec.md

# Con Spec Kit CLI
spec-kit init --template docs/specs/renovatio-spec-template.md \
              --output docs/specs/activas/mi-spec.md
```

### Validar
```bash
spec-kit validate docs/specs/activas/mi-spec.md
```

### Sincronizar con GitHub
```bash
export GITHUB_TOKEN="ghp_xxxx"
spec-kit sync --create-issues docs/specs/activas/mi-spec.md
```

### Actualizar Estado
```bash
spec-kit update --status in-progress docs/specs/activas/mi-spec.md
```

---

## 📝 Metadatos Renovatio

```yaml
---
title: "Migración COBOL a Java"
status: "draft" # draft | in-review | approved | in-progress | completed
version: "1.0.0"
author: "tu-nombre"

renovatio:
  source_language: "cobol"
  target_language: "java"
  
  tools_sequence:
    - name: "cobol.analyze"
      params:
        workspacePath: "/path/to/workspace"
    - name: "cobol.plan"
      params:
        workspacePath: "/path/to/workspace"
    - name: "cobol.apply"
      params:
        planId: "${PLAN_ID}"
        dryRun: false
  
  expected_outcomes:
    files_migrated: 15
    success_rate: ">= 95%"
---
```

---

## 🔧 Herramientas MCP Renovatio

### COBOL Tools
```bash
# Analizar workspace COBOL
curl -X POST http://localhost:8081/mcp -d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "cobol.analyze",
    "arguments": {"workspacePath": "/path/to/cobol"}
  }
}'

# Calcular métricas
cobol.metrics

# Crear plan de migración
cobol.plan

# Aplicar migración
cobol.apply

# Generar diff
cobol.diff
```

### Java Tools
```bash
# Analizar código Java
java.analyze

# Planificar refactoring
java.plan

# Aplicar recetas OpenRewrite
java.apply

# Formatear código
java.format

# Ejecutar tests
java.test

# Recoger métricas
java.metrics
```

---

## 📊 Estados de Especificación

| Estado | Significado | Comando |
|--------|-------------|---------|
| `draft` | Borrador inicial | - |
| `in-review` | En revisión | `spec-kit update --status in-review` |
| `approved` | Aprobada | `spec-kit update --status approved` |
| `in-progress` | En ejecución | `spec-kit update --status in-progress` |
| `completed` | Finalizada | `spec-kit update --status completed` |
| `on-hold` | En pausa | `spec-kit update --status on-hold` |
| `cancelled` | Cancelada | `spec-kit update --status cancelled` |

---

## 📁 Estructura de Directorios

```
docs/specs/
├── renovatio-spec-template.md     # Plantilla base
├── ejemplos/                       # Ejemplos para copiar
│   └── migracion-cobol-basica.md
├── activas/                        # Specs en progreso
│   └── mi-proyecto.md
└── completadas/                    # Specs finalizadas
    └── proyecto-completado.md
```

---

## 🔍 Búsqueda y Filtrado

```bash
# Buscar por lenguaje
grep -l "source_language: \"cobol\"" docs/specs/**/*.md

# Buscar por estado
grep -l "status: \"in-progress\"" docs/specs/activas/*.md

# Buscar por herramienta
grep -l "cobol.analyze" docs/specs/**/*.md

# Listar specs activas
ls docs/specs/activas/

# Listar specs completadas
ls docs/specs/completadas/
```

---

## 🚦 Flujo de Trabajo

```
1. Crear    → cp ejemplo → personalizar → validar
2. Revisar  → spec-kit update --status in-review
3. Aprobar  → spec-kit update --status approved
4. Ejecutar → renovatio tools → actualizar progreso
5. Validar  → tests → métricas → verificar
6. Cerrar   → spec-kit update --status completed → archivar
```

---

## 🔐 Configuración GitHub Token

```bash
# Crear token en: https://github.com/settings/tokens
# Permisos necesarios: repo, project

# Configurar
export GITHUB_TOKEN="ghp_xxxxxxxxxxxx"

# Verificar
curl -H "Authorization: token $GITHUB_TOKEN" \
     https://api.github.com/user
```

---

## 📖 Documentación Rápida

| Documento | Propósito | Tiempo |
|-----------|-----------|--------|
| [SPEC-KIT-QUICK-START.md](./SPEC-KIT-QUICK-START.md) | Guía rápida | 5 min |
| [EXPLICACION-SPEC-KIT.md](./EXPLICACION-SPEC-KIT.md) | Explicación completa | 30 min |
| [specs/ejemplos/](./specs/ejemplos/) | Ejemplos prácticos | 15 min |
| [specs/INDEX.md](./specs/INDEX.md) | Índice central | 10 min |

---

## ⚡ Atajos Comunes

### Crear spec COBOL básica
```bash
cp docs/specs/ejemplos/migracion-cobol-basica.md \
   docs/specs/activas/$(date +%Y%m%d)-mi-migracion.md
```

### Validar todas las specs activas
```bash
for spec in docs/specs/activas/*.md; do
  spec-kit validate "$spec"
done
```

### Actualizar múltiples specs
```bash
spec-kit update --status in-progress docs/specs/activas/*.md
```

### Mover a completadas
```bash
mv docs/specs/activas/mi-spec.md docs/specs/completadas/
```

---

## 🐛 Troubleshooting

### Spec Kit no encontrado
```bash
which spec-kit
npm list -g @github/spec-kit
npm install -g @github/spec-kit
```

### Error de validación
```bash
spec-kit validate --verbose mi-spec.md
```

### GitHub sync falla
```bash
# Verificar token
echo $GITHUB_TOKEN

# Verificar permisos
curl -H "Authorization: token $GITHUB_TOKEN" \
     https://api.github.com/user
```

### Renovatio MCP no responde
```bash
# Verificar servidor
curl http://localhost:8081/mcp/health

# Ver logs
tail -f renovatio-mcp-server.log
```

---

## 💡 Tips Rápidos

✅ **Siempre validar** antes de compartir
✅ **Usar ejemplos** como punto de partida
✅ **Documentar resultados** en la spec
✅ **Actualizar estado** regularmente
✅ **Archivar al completar**

❌ **No incluir credenciales**
❌ **No crear specs muy amplias**
❌ **No olvidar sincronizar**

---

## 🔗 Enlaces Rápidos

- [GitHub Spec Kit](https://github.com/github/spec-kit)
- [Renovatio README](../README.md)
- [MCP Guide](../MCP-CLIENT-GUIDE.md)
- [Ejemplos](./specs/ejemplos/)

---

**Última actualización**: 2025-01-15
