# Renovatio MCP Quick Reference

Referencia rápida para usar el servidor MCP de Renovatio con Java y COBOL.

## 🚀 Configuración Rápida

### Java Projects
```json
{
  "mcpServers": {
    "renovatio-java": {
      "command": "/path/to/renovatio/run_mcp_stdio_server.sh",
      "env": { "RENOVATIO_DEFAULT_LANGUAGE": "java" }
    }
  }
}
```

### COBOL Projects
```json
{
  "mcpServers": {
    "renovatio-cobol": {
      "command": "/path/to/renovatio/run_mcp_stdio_server.sh",
      "env": { "RENOVATIO_DEFAULT_LANGUAGE": "cobol" }
    }
  }
}
```

### Mixed Projects
```json
{
  "mcpServers": {
    "renovatio": {
      "command": "/path/to/renovatio/run_mcp_stdio_server.sh"
    }
  }
}
```

## 🛠️ Java Tools

| Tool | Description |
|------|-------------|
| `java.analyze` | Analyze Java sources |
| `java.format` | Format code & remove unused imports |
| `java.test` | Run project tests |
| `java.metrics` | Collect code metrics |
| `java.recipe_list` | List available OpenRewrite recipes |
| `java.<recipeId>` | Execute OpenRewrite recipe |

### Example: Analyze Java Code
```json
{
  "method": "tools/call",
  "params": {
    "name": "java.analyze",
    "arguments": {
      "workspacePath": "/home/user/my-project"
    }
  }
}
```

## 🏢 COBOL Tools

| Tool | Description |
|------|-------------|
| `cobol.analyze` | Analyze COBOL sources |
| `cobol.metrics` | Collect COBOL metrics |
| `cobol.plan` | Create COBOL→Java migration plan |
| `cobol.apply` | Apply migration plan |
| `cobol.diff` | Generate migration diff |
| `cobol.migrate_copybook` | Generate Java from copybook |
| `cobol.migrate_db2` | Generate JPA from EXEC SQL |

### Example: Migrate Copybook
```json
{
  "method": "tools/call",
  "params": {
    "name": "cobol.migrate_copybook",
    "arguments": {
      "workspacePath": "/home/user/cobol-project",
      "copybook": "CUSTOMER.cpy"
    }
  }
}
```

## 🔍 Language Filtering

### Initialize with Filter
```json
{
  "method": "initialize",
  "params": { "language": "java" }
}
```

### List Tools with Filter
```json
{
  "method": "tools/list",
  "params": { "language": "cobol" }
}
```

### List All Tools (No Filter)
```json
{
  "method": "tools/list"
}
```

## 📁 Required Arguments

All tools require `workspacePath` (absolute path):
```json
{
  "arguments": {
    "workspacePath": "/absolute/path/to/workspace"
  }
}
```

## ❌ Common Errors

| Error Code | Description | Solution |
|------------|-------------|----------|
| `-32601` | Method not found | Check tool name (case-sensitive) |
| `-32602` | Invalid params | Ensure `workspacePath` is provided |
| `-32603` | Internal error | Check server logs |

## 📚 More Information

- **[MCP Client Guide](./MCP-CLIENT-GUIDE.md)** - Complete usage guide
- **[Examples Directory](./examples/)** - Configuration examples
- **[README.md](./README.md)** - Full documentation

## 🔗 Useful Links

- [Model Content Protocol Specification](https://modelcontextprotocol.io/)
- [OpenRewrite Documentation](https://docs.openrewrite.org/)
- [JSON-RPC 2.0 Specification](https://www.jsonrpc.org/specification)

---

**Renovatio** – Multi-language refactoring platform with full MCP support
