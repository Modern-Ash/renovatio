# Renovatio MCP Client Guide

Esta guía explica cómo usar el servidor MCP de Renovatio de forma práctica desde clientes MCP, con soporte completo para Java y COBOL según el estándar Model Content Protocol.

## Tabla de Contenidos

1. [Visión General](#visión-general)
2. [Configuración del Cliente](#configuración-del-cliente)
3. [Filtrado por Lenguaje](#filtrado-por-lenguaje)
4. [Ejemplos Prácticos](#ejemplos-prácticos)
5. [Mejores Prácticas](#mejores-prácticas)
6. [Troubleshooting](#troubleshooting)

---

## Visión General

Renovatio expone herramientas MCP para **Java** y **COBOL** siguiendo el estándar Model Content Protocol. Los clientes pueden:

1. **Listar todas las herramientas** disponibles (Java + COBOL)
2. **Filtrar herramientas por lenguaje** específico
3. **Ejecutar herramientas** con parámetros específicos del lenguaje
4. **Cambiar de lenguaje dinámicamente** según el contexto del proyecto

---

## Configuración del Cliente

### Configuración Básica (VS Code)

Crea o edita el archivo de configuración MCP del cliente (ej. `.vscode/mcp-config.json` o `~/mcp-servers.json`):

```json
{
  "mcpServers": {
    "renovatio": {
      "command": "/path/to/renovatio/run_mcp_stdio_server.sh",
      "args": [],
      "env": {
        "PATH": "/usr/bin:/bin:/usr/local/bin",
        "JAVA_HOME": "/usr/lib/jvm/java-17-openjdk"
      }
    }
  }
}
```

### Configuración con Preferencia de Lenguaje

Para proyectos específicos de un lenguaje, puedes preconfigurar el lenguaje preferido:

#### Configuración para Proyectos Java

```json
{
  "mcpServers": {
    "renovatio-java": {
      "command": "/path/to/renovatio/run_mcp_stdio_server.sh",
      "args": ["--language", "java"],
      "env": {
        "RENOVATIO_DEFAULT_LANGUAGE": "java"
      }
    }
  }
}
```

#### Configuración para Proyectos COBOL

```json
{
  "mcpServers": {
    "renovatio-cobol": {
      "command": "/path/to/renovatio/run_mcp_stdio_server.sh",
      "args": ["--language", "cobol"],
      "env": {
        "RENOVATIO_DEFAULT_LANGUAGE": "cobol"
      }
    }
  }
}
```

#### Configuración Multi-Lenguaje

Para proyectos que mezclan Java y COBOL (migración):

```json
{
  "mcpServers": {
    "renovatio-full": {
      "command": "/path/to/renovatio/run_mcp_stdio_server.sh",
      "args": [],
      "description": "Full stack refactoring - Java + COBOL"
    }
  }
}
```

---

## Filtrado por Lenguaje

### Durante la Inicialización

Al inicializar la conexión MCP, especifica el lenguaje para recibir solo herramientas relevantes:

```json
{
  "jsonrpc": "2.0",
  "id": "1",
  "method": "initialize",
  "params": {
    "language": "java",
    "clientInfo": {
      "name": "VSCode",
      "version": "1.0.0"
    }
  }
}
```

**Respuesta** - Solo herramientas Java:

```json
{
  "jsonrpc": "2.0",
  "id": "1",
  "result": {
    "protocolVersion": "2025-06-18",
    "capabilities": { ... },
    "serverInfo": {
      "name": "Renovatio MCP Server",
      "version": "1.0.0"
    },
    "availableTools": [
      {
        "name": "java.analyze",
        "description": "Analyze Java sources using OpenRewrite recipes"
      },
      {
        "name": "java.format",
        "description": "Format Java sources and remove unused imports"
      }
      // ... más herramientas Java
    ]
  }
}
```

### Al Listar Herramientas

Puedes filtrar herramientas dinámicamente al listarlas:

```json
{
  "jsonrpc": "2.0",
  "id": "2",
  "method": "tools/list",
  "params": {
    "language": "cobol"
  }
}
```

**Respuesta** - Solo herramientas COBOL:

```json
{
  "jsonrpc": "2.0",
  "id": "2",
  "result": {
    "tools": [
      {
        "name": "cobol.analyze",
        "description": "Analyze COBOL sources (parsing, AST, dependencies)"
      },
      {
        "name": "cobol.plan",
        "description": "Create migration plan from COBOL to Java"
      },
      {
        "name": "cobol.migrate_copybook",
        "description": "Generate Java artifacts from a COBOL copybook"
      }
      // ... más herramientas COBOL
    ]
  }
}
```

### Sin Filtro (Todas las Herramientas)

Si omites el parámetro `language`, recibirás todas las herramientas:

```json
{
  "jsonrpc": "2.0",
  "id": "3",
  "method": "tools/list"
}
```

---

## Ejemplos Prácticos

### Ejemplo 1: Análisis de Código Java

```json
{
  "jsonrpc": "2.0",
  "id": "10",
  "method": "tools/call",
  "params": {
    "name": "java.analyze",
    "arguments": {
      "workspacePath": "/home/user/my-java-project",
      "nql": "FIND classes WHERE package startsWith 'com.example'"
    }
  }
}
```

**Respuesta:**

```json
{
  "jsonrpc": "2.0",
  "id": "10",
  "result": {
    "success": true,
    "summary": "java.analyze: analyzed 15 files (23 classes, 145 methods, 42 unique imports) in 1234 ms.",
    "data": {
      "type": "analyze",
      "metrics": {
        "totalFiles": 15,
        "totalClasses": 23,
        "totalMethods": 145
      },
      "issues": [],
      "analyzedFiles": [
        "/home/user/my-java-project/src/main/java/com/example/UserService.java",
        "/home/user/my-java-project/src/main/java/com/example/OrderService.java"
      ]
    }
  }
}
```

### Ejemplo 2: Análisis de Código COBOL

```json
{
  "jsonrpc": "2.0",
  "id": "11",
  "method": "tools/call",
  "params": {
    "name": "cobol.analyze",
    "arguments": {
      "workspacePath": "/home/user/cobol-legacy",
      "nql": "FIND programs WHERE copybooks INCLUDE 'CUSTOMER'"
    }
  }
}
```

### Ejemplo 3: Migración de Copybook COBOL a Java

```json
{
  "jsonrpc": "2.0",
  "id": "12",
  "method": "tools/call",
  "params": {
    "name": "cobol.migrate_copybook",
    "arguments": {
      "workspacePath": "/home/user/cobol-legacy",
      "copybook": "CUSTOMER.cpy"
    }
  }
}
```

**Respuesta:**

```json
{
  "jsonrpc": "2.0",
  "id": "12",
  "result": {
    "success": true,
    "message": "Generated 3 artifacts",
    "data": {
      "generated": {
        "CustomerDTO.java": "package com.example.dto;\n\npublic class CustomerDTO { ... }",
        "CustomerMapper.java": "package com.example.mapper;\n\npublic class CustomerMapper { ... }",
        "CustomerEntity.java": "package com.example.entity;\n\npublic class CustomerEntity { ... }"
      }
    }
  }
}
```

### Ejemplo 4: Plan de Migración COBOL → Java

```json
{
  "jsonrpc": "2.0",
  "id": "13",
  "method": "tools/call",
  "params": {
    "name": "cobol.plan",
    "arguments": {
      "workspacePath": "/home/user/cobol-legacy",
      "scope": "**/*.cbl",
      "goals": ["db2", "jpa", "rest"]
    }
  }
}
```

### Ejemplo 5: Formateo de Código Java

```json
{
  "jsonrpc": "2.0",
  "id": "14",
  "method": "tools/call",
  "params": {
    "name": "java.format",
    "arguments": {
      "workspacePath": "/home/user/my-java-project"
    }
  }
}
```

### Ejemplo 6: Aplicar Recipe de OpenRewrite

```json
{
  "jsonrpc": "2.0",
  "id": "15",
  "method": "tools/call",
  "params": {
    "name": "java.org.openrewrite.java.format.AutoFormat",
    "arguments": {
      "workspacePath": "/home/user/my-java-project"
    }
  }
}
```

---

## Mejores Prácticas

### 1. Uso del Parámetro `language`

**✅ Recomendado para:**
- Proyectos monolíticos (solo Java o solo COBOL)
- Reducir la carga cognitiva del usuario (menos herramientas en la lista)
- Mejorar el rendimiento al listar herramientas

**❌ No recomendado para:**
- Proyectos de migración que mezclan ambos lenguajes
- Exploraciones o pruebas de diferentes herramientas

### 2. Convención de Nombres de Herramientas

Todas las herramientas siguen el patrón `<lenguaje>.<acción>`:

- **Java**: `java.analyze`, `java.format`, `java.test`, etc.
- **COBOL**: `cobol.analyze`, `cobol.plan`, `cobol.migrate_copybook`, etc.

Los clientes pueden filtrar por prefijo `java.` o `cobol.` si implementan su propia lógica de filtrado.

### 3. Workspace Path

Siempre especifica rutas absolutas en `workspacePath`:

```json
{
  "workspacePath": "/home/user/my-project"  // ✅ Correcto
}
```

```json
{
  "workspacePath": "./my-project"  // ❌ Evitar rutas relativas
}
```

### 4. Manejo de Errores

Renovatio devuelve errores MCP estándar:

```json
{
  "jsonrpc": "2.0",
  "id": "20",
  "error": {
    "code": -32602,
    "message": "Invalid params - missing tool name"
  }
}
```

Códigos de error comunes:
- `-32600`: Invalid Request
- `-32601`: Method not found
- `-32602`: Invalid params
- `-32603`: Internal error

### 5. Descubrimiento Dinámico de Recipes

Las recetas de OpenRewrite se exponen dinámicamente como herramientas MCP con el patrón `java.<recipeId>`.

Para listar recipes disponibles:

```json
{
  "jsonrpc": "2.0",
  "id": "21",
  "method": "tools/call",
  "params": {
    "name": "java.recipe_list",
    "arguments": {
      "workspacePath": "/home/user/my-java-project"
    }
  }
}
```

---

## Troubleshooting

### Problema: No se listan herramientas para un lenguaje

**Solución:**
1. Verificar que el módulo provider esté en el classpath:
   ```bash
   mvn clean install
   ```
2. Confirmar que el lenguaje está registrado:
   ```json
   {
     "method": "server/info"
   }
   ```
   Revisa `supportedLanguages` en la respuesta.

### Problema: Error "Tool not found"

**Solución:**
1. Listar herramientas disponibles:
   ```json
   { "method": "tools/list" }
   ```
2. Verificar el nombre exacto de la herramienta (case-sensitive).
3. Para recipes de OpenRewrite, usar el nombre completo: `java.org.openrewrite.java.format.AutoFormat`

### Problema: "workspacePath is required"

**Solución:**
Todas las herramientas requieren `workspacePath` como argumento. Asegúrate de incluirlo en `arguments`.

### Problema: Conflicto entre herramientas Java y COBOL

**Solución:**
Si tienes proyectos separados:
1. Crea instancias MCP separadas con filtros de lenguaje
2. Usa workspaces diferentes para cada proyecto
3. Aprovecha el filtro `language` en `initialize` y `tools/list`

---

## Integración con IDEs

### VS Code

Instala una extensión MCP compatible (ej. Copilot Workspace) y configura:

```json
{
  "mcpServers": {
    "renovatio": {
      "command": "/path/to/renovatio/run_mcp_stdio_server.sh"
    }
  }
}
```

### IntelliJ IDEA

Usa un plugin MCP para IntelliJ (ej. MCP Client Plugin) y configura el servidor como un servicio externo.

### CLI

Usa herramientas como `mcp-client-cli` para interactuar desde línea de comandos:

```bash
mcp-client-cli --server /path/to/renovatio/run_mcp_stdio_server.sh \
  --method tools/call \
  --params '{"name": "java.analyze", "arguments": {"workspacePath": "/home/user/project"}}'
```

---

## Recursos Adicionales

- [Model Content Protocol Specification](https://modelcontextprotocol.io/)
- [OpenRewrite Documentation](https://docs.openrewrite.org/)
- [Renovatio Architecture](./ARCHITECTURE.md)
- [Renovatio README](./README.md)

---

## Resumen de Herramientas por Lenguaje

### Herramientas Java

| Herramienta | Descripción |
|-------------|-------------|
| `java.analyze` | Analizar fuentes Java con OpenRewrite |
| `java.format` | Formatear código y eliminar imports no usados |
| `java.test` | Ejecutar tests del proyecto |
| `java.metrics` | Recopilar métricas de código Java |
| `java.recipe_list` | Listar recipes de OpenRewrite disponibles |
| `java.recipe_describe` | Describir una recipe específica |
| `java.<recipeId>` | Ejecutar una recipe de OpenRewrite |

### Herramientas COBOL

| Herramienta | Descripción |
|-------------|-------------|
| `cobol.analyze` | Analizar fuentes COBOL (parsing, AST, dependencias) |
| `cobol.metrics` | Recopilar métricas de código COBOL |
| `cobol.plan` | Crear plan de migración de COBOL a Java |
| `cobol.apply` | Aplicar plan de migración |
| `cobol.diff` | Generar diff de última migración |
| `cobol.migrate_copybook` | Generar artefactos Java desde copybook COBOL |
| `cobol.migrate_db2` | Generar código JPA desde SQL embebido en COBOL |

---

**Renovatio** – Plataforma de refactoring multi-lenguaje con soporte MCP completo.
