# Integración Spec Kit + Renovatio - Diagrama de Flujo

## Visión General de la Integración

```mermaid
graph TB
    subgraph "GitHub Spec Kit"
        A[Crear Especificación]
        B[Validar Spec]
        C[Sincronizar con GitHub]
    end
    
    subgraph "GitHub"
        D[Issues]
        E[Projects]
        F[Pull Requests]
    end
    
    subgraph "Renovatio MCP Server"
        G[tools/list]
        H[tools/call]
        I[Provider COBOL]
        J[Provider Java]
    end
    
    subgraph "Ejecución"
        K[cobol.analyze]
        L[cobol.plan]
        M[cobol.apply]
        N[java.analyze]
        O[java.plan]
        P[java.apply]
    end
    
    subgraph "Resultados"
        Q[Código Generado]
        R[Tests]
        S[Métricas]
        T[Diffs]
    end
    
    A -->|spec-kit validate| B
    B -->|spec-kit sync| C
    C --> D
    C --> E
    
    B -->|lee metadatos renovatio| G
    G --> H
    
    H --> I
    H --> J
    
    I --> K
    I --> L
    I --> M
    
    J --> N
    J --> O
    J --> P
    
    K --> S
    L --> T
    M --> Q
    M --> R
    
    N --> S
    O --> T
    P --> Q
    P --> R
    
    Q -->|actualiza| A
    R -->|actualiza| A
    S -->|actualiza| A
    T -->|actualiza| A
```

---

## Flujo de Trabajo Detallado

### 1. Creación de Especificación

```mermaid
sequenceDiagram
    participant Dev as Desarrollador
    participant Template as Plantilla
    participant SpecKit as Spec Kit CLI
    participant Spec as Especificación.md
    
    Dev->>Template: Selecciona plantilla
    Template->>SpecKit: spec-kit init
    SpecKit->>Spec: Genera nueva spec
    Dev->>Spec: Personaliza contenido
    Dev->>Spec: Añade metadatos renovatio
```

### 2. Validación y Sincronización

```mermaid
sequenceDiagram
    participant Dev as Desarrollador
    participant SpecKit as Spec Kit CLI
    participant Spec as Especificación.md
    participant GitHub as GitHub API
    participant Issues as GitHub Issues
    
    Dev->>SpecKit: spec-kit validate
    SpecKit->>Spec: Lee y valida
    SpecKit-->>Dev: ✅ Válida
    
    Dev->>SpecKit: spec-kit sync --create-issues
    SpecKit->>Spec: Extrae tareas
    SpecKit->>GitHub: Crea issues
    GitHub->>Issues: Issues creados
    GitHub-->>Dev: Links a issues
```

### 3. Ejecución con Renovatio

```mermaid
sequenceDiagram
    participant Dev as Desarrollador
    participant Spec as Especificación.md
    participant Script as Script Automatización
    participant MCP as Renovatio MCP Server
    participant Provider as Provider (COBOL/Java)
    participant Output as Código Generado
    
    Dev->>Spec: Aprobar (status: approved)
    Dev->>Script: Ejecutar automatización
    Script->>Spec: Lee tools_sequence
    
    loop Para cada herramienta
        Script->>MCP: tools/call
        MCP->>Provider: Ejecutar herramienta
        Provider->>Output: Generar código
        Provider-->>MCP: Resultado
        MCP-->>Script: Respuesta
        Script->>Spec: Actualizar progreso
    end
    
    Script-->>Dev: Ejecución completa
```

### 4. Validación y Cierre

```mermaid
sequenceDiagram
    participant Dev as Desarrollador
    participant Output as Código Generado
    participant Tests as Tests
    participant Spec as Especificación.md
    participant SpecKit as Spec Kit CLI
    participant GitHub as GitHub Issues
    
    Dev->>Output: Revisar código
    Dev->>Tests: Ejecutar tests
    Tests-->>Dev: ✅ Tests pasan
    
    Dev->>Spec: Documentar resultados
    Dev->>Spec: Actualizar métricas
    Dev->>SpecKit: spec-kit update --status completed
    SpecKit->>Spec: Actualizar estado
    SpecKit->>GitHub: Cerrar issues
    
    Dev->>Spec: Mover a completadas/
```

---

## Integración de Metadatos

### Metadatos Spec Kit + Renovatio

```yaml
---
# Metadatos estándar Spec Kit
title: "Migración COBOL a Java"
status: "approved"
version: "1.0.0"
author: "equipo-dev"
reviewers: ["arquitecto", "tech-lead"]

# Metadatos específicos de Renovatio
renovatio:
  source_language: "cobol"
  target_language: "java"
  
  # Secuencia de herramientas MCP
  tools_sequence:
    - name: "cobol.analyze"
      params:
        workspacePath: "/workspace/cobol"
      expected_output:
        programs_found: ">= 10"
        
    - name: "cobol.plan"
      params:
        workspacePath: "/workspace/cobol"
        targetLanguage: "java"
      expected_output:
        plan_id: "plan-*"
        
    - name: "cobol.apply"
      params:
        planId: "${PLAN_ID}"  # Del paso anterior
        dryRun: false
      expected_output:
        files_generated: ">= 10"
        
  # Resultados esperados
  expected_outcomes:
    success_rate: ">= 95%"
    compilation_errors: 0
    test_coverage: ">= 70%"
---
```

---

## Automatización Completa

### Script de Ejecución Automática

```bash
#!/bin/bash
# execute-spec-with-renovatio.sh

SPEC_FILE=$1
MCP_SERVER="http://localhost:8081/mcp"

# 1. Validar especificación
echo "Validando especificación..."
spec-kit validate "$SPEC_FILE" || exit 1

# 2. Extraer metadatos renovatio (usando yq o similar)
echo "Extrayendo secuencia de herramientas..."
TOOLS_SEQUENCE=$(yq eval '.renovatio.tools_sequence' "$SPEC_FILE" -o json)

# 3. Ejecutar cada herramienta
echo "$TOOLS_SEQUENCE" | jq -c '.[]' | while read -r tool; do
    TOOL_NAME=$(echo "$tool" | jq -r '.name')
    TOOL_PARAMS=$(echo "$tool" | jq -c '.params')
    
    echo "Ejecutando $TOOL_NAME..."
    
    # Llamada MCP
    RESULT=$(curl -s -X POST "$MCP_SERVER" \
        -H "Content-Type: application/json" \
        -d "{
            \"jsonrpc\": \"2.0\",
            \"id\": \"$(uuidgen)\",
            \"method\": \"tools/call\",
            \"params\": {
                \"name\": \"$TOOL_NAME\",
                \"arguments\": $TOOL_PARAMS
            }
        }")
    
    # Verificar resultado
    if echo "$RESULT" | jq -e '.error' > /dev/null; then
        echo "❌ Error en $TOOL_NAME"
        echo "$RESULT" | jq '.error'
        exit 1
    fi
    
    echo "✅ $TOOL_NAME completado"
    
    # Guardar salida para siguiente herramienta (ej: PLAN_ID)
    if [ "$TOOL_NAME" == "cobol.plan" ]; then
        export PLAN_ID=$(echo "$RESULT" | jq -r '.result.planId')
        echo "Plan ID: $PLAN_ID"
    fi
done

# 4. Actualizar estado de la spec
echo "Actualizando estado de la especificación..."
spec-kit update --status completed "$SPEC_FILE"

echo "✅ Ejecución completa"
```

---

## Arquitectura de Integración

```
┌─────────────────────────────────────────────────────┐
│                 GitHub Spec Kit                      │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │ Templates    │  │ CLI Tools    │  │ Metadata  │ │
│  └──────────────┘  └──────────────┘  └───────────┘ │
└────────────────────┬────────────────────────────────┘
                     │ specs/*.md
                     │ + renovatio metadata
                     ▼
┌─────────────────────────────────────────────────────┐
│            Automation Layer (Scripts)                │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │ Parser       │  │ Orchestrator │  │ Reporter  │ │
│  └──────────────┘  └──────────────┘  └───────────┘ │
└────────────────────┬────────────────────────────────┘
                     │ JSON-RPC 2.0
                     ▼
┌─────────────────────────────────────────────────────┐
│              Renovatio MCP Server                    │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │ Protocol     │  │ Tool Router  │  │ Providers │ │
│  └──────────────┘  └──────────────┘  └───────────┘ │
└────────────────────┬────────────────────────────────┘
                     │
          ┌──────────┴──────────┐
          ▼                     ▼
┌──────────────────┐  ┌──────────────────┐
│ COBOL Provider   │  │ Java Provider    │
│                  │  │                  │
│ • analyze        │  │ • analyze        │
│ • plan           │  │ • plan           │
│ • apply          │  │ • apply          │
│ • metrics        │  │ • format         │
└──────────────────┘  └──────────────────┘
          │                     │
          └──────────┬──────────┘
                     ▼
          ┌──────────────────────┐
          │   Generated Code     │
          │   + Tests + Docs     │
          └──────────────────────┘
```

---

## Beneficios de la Integración

### 1. Trazabilidad Completa
```
Spec → Issues → Code → Tests → Metrics → Spec
```

### 2. Automatización
```
Manual Process: 2-3 días
Con Spec Kit + Renovatio: 2-3 horas
```

### 3. Reutilización
```
1 spec exitosa → plantilla → N proyectos similares
```

### 4. Calidad
```
Validación automática + Tests + Métricas = Alta calidad
```

---

## Próximos Pasos

1. **Fase 1**: Usar specs manualmente con Renovatio
2. **Fase 2**: Crear scripts de automatización básicos
3. **Fase 3**: Integración GitHub Actions
4. **Fase 4**: Dashboard y métricas en tiempo real

---

**Recursos:**
- [EXPLICACION-SPEC-KIT.md](./EXPLICACION-SPEC-KIT.md) - Explicación completa
- [SPEC-KIT-QUICK-START.md](./SPEC-KIT-QUICK-START.md) - Guía rápida
- [specs/INDEX.md](./specs/INDEX.md) - Índice de especificaciones
