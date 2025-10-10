# Renovatio MCP Configuration Examples

Este directorio contiene ejemplos de configuración para diferentes escenarios de uso del servidor MCP de Renovatio.

## Archivos de Configuración

### `vscode-java-only.json`
Configuración para proyectos **exclusivamente Java**. Filtra las herramientas COBOL para una experiencia más limpia.

**Cuándo usar:**
- Proyectos de refactoring Java puros
- Modernización de aplicaciones Java
- Análisis de código Java con OpenRewrite

### `vscode-cobol-only.json`
Configuración para proyectos **exclusivamente COBOL** o migraciones COBOL → Java.

**Cuándo usar:**
- Análisis de código COBOL legacy
- Migración de COBOL a Java
- Generación de stubs desde copybooks

### `vscode-multi-language.json`
Configuración con **todas las herramientas** (Java + COBOL) disponibles.

**Cuándo usar:**
- Proyectos de migración activa COBOL → Java
- Necesitas acceso a herramientas de ambos lenguajes
- Exploraciones y pruebas de diferentes herramientas

### `vscode-multiple-instances.json`
Configuración con **múltiples instancias** del servidor MCP, cada una con un propósito específico.

**Cuándo usar:**
- Trabajas en múltiples proyectos con diferentes lenguajes
- Quieres separar claramente las herramientas por contexto
- Prefieres flexibilidad para cambiar entre configuraciones

## Cómo Usar

### VS Code

1. Copia el archivo de configuración que necesites
2. Renómbralo a `.vscode/mcp-config.json` en tu proyecto, o a `~/mcp-servers.json` para configuración global
3. Actualiza el valor de `command` con la ruta correcta a tu script de inicio de Renovatio:
   ```json
   "command": "/absolute/path/to/renovatio/run_mcp_stdio_server.sh"
   ```
4. Recarga VS Code o reinicia la extensión MCP

### Copilot Workspace

1. Usa la configuración del archivo correspondiente en tu workspace settings
2. Asegúrate de que el servidor MCP esté configurado correctamente

### Otros Clientes MCP

Adapta la estructura JSON según las especificaciones de tu cliente MCP. Todos siguen el estándar Model Content Protocol.

## Variables de Entorno

Todas las configuraciones de ejemplo incluyen:

- **`PATH`**: Rutas de sistema necesarias
- **`JAVA_HOME`**: Ruta a la instalación de Java 17+
- **`RENOVATIO_DEFAULT_LANGUAGE`** (opcional): Filtro de lenguaje predeterminado (`java` o `cobol`)
  - Cuando se configura, el servidor filtrará automáticamente las herramientas al lenguaje especificado
  - Las peticiones MCP pueden sobrescribir este valor usando el parámetro `language`
  - Si no se configura, el servidor expone todas las herramientas de todos los lenguajes

## Personalización

Puedes personalizar cualquier configuración agregando:

- **Argumentos adicionales** en el array `args`
- **Variables de entorno** personalizadas en `env`
- **Descripción** en el campo `description` para identificar fácilmente cada instancia

Ejemplo:
```json
{
  "mcpServers": {
    "my-custom-renovatio": {
      "command": "/path/to/renovatio/run_mcp_stdio_server.sh",
      "args": ["--verbose", "--log-level=DEBUG"],
      "env": {
        "PATH": "/usr/bin:/bin",
        "JAVA_HOME": "/usr/lib/jvm/java-17",
        "RENOVATIO_DEFAULT_LANGUAGE": "java",
        "RENOVATIO_WORKSPACE": "/home/user/my-project"
      },
      "description": "Custom Renovatio instance with debug logging"
    }
  }
}
```

## Validación de Esquema

Todas las configuraciones de ejemplo incluyen una referencia al esquema JSON en `../schemas/mcp-config-schema.json`, que proporciona:

- **Autocompletado** en editores compatibles (VS Code, IntelliJ IDEA)
- **Validación** de la estructura de configuración
- **Documentación inline** de cada campo

## Más Información

- [MCP Client Guide](../MCP-CLIENT-GUIDE.md) - Guía completa de uso del cliente MCP
- [README.md](../README.md) - Documentación principal de Renovatio
- [ARCHITECTURE.md](../ARCHITECTURE.md) - Arquitectura del sistema
