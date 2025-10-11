# Guía para usar GitHub Spec Kit con Renovatio

Esta guía explica cómo aprovechar [GitHub Spec Kit](https://github.com/github/spec-kit) para documentar y coordinar iniciativas de modernización dentro del proyecto Renovatio. El objetivo es producir especificaciones claras que puedan convertirse rápidamente en issues, planes MCP y tareas de desarrollo.

> **📖 Para una explicación completa de qué es Spec Kit y cómo puede mejorar Renovatio**, consulta [EXPLICACION-SPEC-KIT.md](./EXPLICACION-SPEC-KIT.md)

## 1. ¿Qué es GitHub Spec Kit?

GitHub Spec Kit es un conjunto de plantillas, utilidades CLI y flujos de trabajo pensados para estandarizar especificaciones funcionales/técnicas. Permite crear "specs" iterables (borrador → revisión → ejecución) con metadatos listos para integrarse en GitHub Projects, Issues o Pull Requests.

> Consulta siempre el README oficial del proyecto para obtener los requisitos y comandos más recientes. Esta guía se enfoca en cómo aplicarlo a Renovatio.

## 2. Preparación del entorno

1. **Clona el repositorio oficial** (usa `git clone` o `gh repo clone`).
2. **Instala las dependencias** siguiendo el README de Spec Kit. El proyecto se publica como una herramienta Node.js/TypeScript, por lo que normalmente necesitarás:
   - Node.js ≥ 18.
   - Corepack habilitado (`corepack enable`) y `pnpm install`, o la alternativa indicada en el README.
3. **Ejecuta las tareas iniciales** sugeridas (por ejemplo `pnpm run build` o `pnpm run dev`). Verifica que el comando `spec-kit` (CLI) quede disponible en `node_modules/.bin` o globalmente según la instalación.
4. **Configura tus tokens** (opcional). Si vas a sincronizar specs con GitHub Projects/Issues, establece las variables de entorno que indique Spec Kit (por ejemplo `GITHUB_TOKEN`).

## 3. Estructurar specs para Renovatio

Cuando generes una especificación con Spec Kit, asegúrate de cubrir los módulos y convenciones de Renovatio:

| Área de Renovatio | Qué documentar en la spec |
|-------------------|---------------------------|
| `renovatio-core` | Nuevas recetas, servicios, integraciones MCP o flujos comunes. |
| `renovatio-provider-java` | Cambios en recetas OpenRewrite, pipelines Java y validaciones. |
| `renovatio-provider-cobol` | Transformaciones COBOL → Java, copybooks, DB2, migraciones batch. |
| `renovatio-mcp-server` | Exposición MCP, schemas JSON, controladores y transporte (HTTP/stdio). |
| `renovatio-shared` | Modelos compartidos, DTOs, contratos y utilidades. |
| `docs/` y `examples/` | Documentación, guías de usuario y configuraciones listas para clientes MCP. |

### Recomendación de estructura

Usa la plantilla [docs/specs/renovatio-spec-template.md](./specs/renovatio-spec-template.md) como punto de partida para cualquier spec nueva. Puedes referenciarla desde Spec Kit mediante el flag `--template` o copiando el contenido al momento de crear el borrador.

La plantilla cubre:
- **Contexto y problema**: por qué el cambio es necesario.
- **Objetivos medibles**: qué métricas o validaciones esperamos.
- **Alcance técnico**: módulos, APIs MCP, recetas y assets afectados.
- **Plan de validación**: pruebas automatizadas, manuales y criterios de aceptación.
- **Checklist**: pasos para coordinar tareas (issues, PRs, pipelines MCP).

## 4. Flujo de trabajo sugerido

1. **Generar borrador**
   - Ejecuta `spec-kit init --template docs/specs/renovatio-spec-template.md` dentro de tu workspace del spec.
   - Completa la información mínima: contexto, objetivos, riesgos y métricas.
2. **Sincronizar con Renovatio**
   - Referencia issues existentes o crea nuevos (uno por módulo si es necesario).
   - Añade enlaces a archivos relevantes (por ejemplo `renovatio-core/src/...`).
   - Define cómo el spec se alinea con las herramientas MCP (`tools/list`, `tools/call`).
3. **Revisión**
   - Asigna revisores y usa las funcionalidades de comentarios del spec.
   - Verifica que el plan incluya pruebas (`mvn test`, `npm test`, workflows de ejemplo) y difusión en la documentación.
4. **Ejecución**
   - Crea issues o tareas en GitHub Projects usando los generadores de Spec Kit.
   - Mantén el spec actualizado con el progreso (checklist, enlaces a PRs, notas de validación).
5. **Cierre**
   - Documenta los resultados: métricas obtenidas, cambios clave y tareas pendientes.
   - Enlaza la spec finalizada desde la documentación de Renovatio (por ejemplo en `docs/` o `MEJORAS-MCP-RESUMEN.md`).

## 5. Integración con MCP y automatizaciones

- **Planes MCP**: convierte secciones del spec en entradas para `tools/plan` y `tools/apply`. Documenta qué herramientas MCP deben ejecutarse y con qué parámetros.
- **Pipelines CI/CD**: incluye en la spec una sección con los comandos que deben agregarse a GitHub Actions u otros pipelines (tests, validación de recetas, generación de diffs).
- **Sincronización bidireccional**: si utilizas el modo Projects de Spec Kit, vincula cada tarea a su issue/PR y actualiza el estado directamente desde el CLI.

## 6. Buenas prácticas

- Mantén las specs en español o inglés según la audiencia. Los identificadores de código deben permanecer en inglés.
- Limita cada spec a un objetivo concreto (por ejemplo "Añadir migración DB2" o "Crear pipeline de validación Java").
- Adjunta ejemplos de entrada/salida o snippets relevantes dentro de bloques de código.
- Guarda las specs dentro de `docs/specs/` para que el equipo las pueda versionar junto al código.
- Cuando cierres una spec, agrega un resumen al changelog o documentación pertinente.

## 7. Recursos adicionales

### Documentación principal

- **[EXPLICACION-SPEC-KIT.md](./EXPLICACION-SPEC-KIT.md)** - Explicación completa de Spec Kit y mejoras para Renovatio
- **[specs/ejemplos/](./specs/ejemplos/)** - Especificaciones de ejemplo listas para usar

### Referencias externas

- [Repositorio oficial de GitHub Spec Kit](https://github.com/github/spec-kit)
- [Documentación de Spec Kit](https://github.com/github/spec-kit#readme)

### Documentación de Renovatio

- Plantilla de Renovatio: [docs/specs/renovatio-spec-template.md](./specs/renovatio-spec-template.md)
- Documentación general del proyecto: [README.md](../README.md), [ARCHITECTURE.md](../ARCHITECTURE.md)

Con esta guía, Renovatio puede aprovechar GitHub Spec Kit para planificar iteraciones de modernización de forma trazable y alineada con el MCP y las herramientas de refactorización existentes.
