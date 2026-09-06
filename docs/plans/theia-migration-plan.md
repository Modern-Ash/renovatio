# Renovatio Workbench sobre Eclipse Theia

## Objetivo

Evolucionar el dashboard actual hacia un workbench tipo IDE para analizar COBOL, proyectar el modelo abstracto de negocio, elegir la arquitectura destino y ejecutar la modernización con trazabilidad, aprobación humana y pruebas de equivalencia.

Theia será la shell de experiencia; el backend Spring Boot, los módulos de dominio, el parser COBOL, el shadow arquitectónico y el harness de equivalencia seguirán siendo servicios de Renovatio.

## Decisión de plataforma

Se recomienda Eclipse Theia, no un fork directo de VS Code.

- Permite producto web y desktop.
- Admite extensiones compatibles con VS Code mediante Open VSX.
- Permite widgets, comandos, paneles, árboles, editores y vistas de dominio propios.
- Theia AI permite agentes, prompts, slash commands, herramientas, MCP, confirmaciones y change sets.
- Evita mantener un fork de la distribución propietaria de VS Code.

## Arquitectura objetivo

```text
renovatio-workbench (Theia)
  ├── renovatio-core-ui
  ├── renovatio-source-explorer
  ├── renovatio-domain-model
  ├── renovatio-architecture-canvas
  ├── renovatio-shadow-diff
  ├── renovatio-equivalence
  ├── renovatio-ai
  └── renovatio-commands

renovatio-api (Spring Boot)
  ├── project/profile API
  ├── source and semantic IR API
  ├── domain model API
  ├── architecture projection API
  ├── artifact manifest API
  ├── equivalence harness API
  ├── LLM evaluation API
  └── audit/history API
```

## Experiencia de usuario

La pantalla principal tendrá cuatro zonas:

1. **Explorer**: proyectos, fuentes COBOL, copybooks, JCL, modelo, arquitectura, runs y evidencias.
2. **Workspace central**: editor COBOL, modelo de dominio, canvas de arquitectura, código Java y diff.
3. **Inspector**: origen, destino, reglas aplicadas, confianza, evidencia, decisiones e impacto.
4. **Bottom panel**: consola, diagnósticos, equivalencia, cambios pendientes e historial.

La navegación será por contexto. Seleccionar un párrafo COBOL debe llevar al elemento de dominio, al componente arquitectónico y al archivo Java correspondiente.

## IA Renovatio

La IA no se presentará como un chat genérico. Se implementarán agentes especializados:

- `Discovery Agent`: inventario y clasificación de COBOL/JCL/copybooks.
- `Domain Architect`: entidades, value objects, agregados, eventos y reglas.
- `Architecture Agent`: MVC, Hexagonal, Clean, Layered y perfiles corporativos.
- `Naming Agent`: paquetes, clases, métodos y convenciones Java.
- `Equivalence Agent`: casos de prueba, entradas, salidas y divergencias.
- `Review Agent`: riesgos, ambigüedades, evidencia y decisiones pendientes.

Cada respuesta debe mostrar: propuesta, evidencia, confianza, impacto, archivos afectados y acción de aprobación. Las acciones de escritura se ejecutarán mediante change sets revisables, nunca directamente desde el chat.

Comandos iniciales:

```text
/analyze-program <program>
/extract-domain
/map-copybook <copybook>
/propose-architecture <profile>
/preview-shadow
/show-impact
/run-equivalence
/explain-divergence <case>
/create-change-set
```

## Plan por fases

### Fase 0 — Fundaciones y spike (1 semana)

- Crear aplicación Theia mínima con workspace local.
- Confirmar empaquetado web y desktop.
- Definir comunicación con `renovatio-api` y autenticación.
- Probar un widget custom, un comando y un árbol de archivos.
- Decidir si el primer release será web-only o web + desktop.

**Salida:** prototipo ejecutable y ADR de plataforma.

### Fase 1 — Shell de workbench (1–2 semanas)

- Migrar navegación del dashboard a Explorer/Activity Bar.
- Añadir tabs, paneles redimensionables y command palette.
- Abrir fuentes COBOL con Monaco/Theia editor.
- Mostrar errores, diagnósticos y estado de proyecto.
- Mantener temporalmente el dashboard React embebido como vista administrativa.

**Criterio:** abrir un proyecto, navegar sus archivos y conservar el flujo actual sin regresiones.

### Fase 2 — Source Explorer y análisis (2 semanas)

- Árbol de programas, copybooks, JCL y ejecuciones.
- Navegación por párrafos, secciones, operaciones SQL/CICS y archivos.
- Panel de símbolos y referencias.
- Integrar parser COBOL e IR semántico.
- Mostrar cobertura de análisis y elementos ambiguos.

**Criterio:** cada elemento visible debe conservar `sourcePath`, posición, identificador y hash.

### Fase 3 — Modelo abstracto de negocio (2–3 semanas)

- Vista de entidades, value objects, agregados, servicios y eventos.
- Editor de relaciones y propiedades.
- Inspector de evidencia COBOL asociada.
- Sugerencias IA con aceptar, editar o rechazar.
- Versionado del modelo y comparación entre versiones.

**Criterio:** el usuario puede modificar el modelo y volver al origen COBOL desde cualquier elemento.

### Fase 4 — Architecture Canvas y perfiles (2–3 semanas)

- Canvas para MVC, Hexagonal, Clean y Layered.
- Perfiles guardables y reutilizables.
- Editor de paquetes, reglas, sufijos y prefijos.
- Restricciones de dependencias y validación en vivo.
- Preview del shadow con rutas, clases y paquetes Java.

**Criterio:** cambiar de MVC a Hexagonal no modifica el modelo de negocio; sólo cambia la proyección y el manifest.

### Fase 5 — Shadow, diff y change sets (2 semanas)

- Vista COBOL → dominio → Java.
- Diff de archivos, paquetes y decisiones.
- Impact analysis antes de generar.
- Change set revisable, aprobable y descartable.
- Exportación de manifest y evidencia.

**Criterio:** ninguna generación modifica archivos sin change set aprobado.

### Fase 6 — IA gobernada (2–3 semanas)

- Integrar Theia AI o una capa equivalente de agentes Renovatio.
- Catálogo de prompts versionados.
- Contexto explícito por selección, archivo y proyecto.
- Tool calls hacia APIs Renovatio y MCP.
- Confirmación humana para acciones mutantes.
- Historial de conversaciones, prompts, modelos y resultados.
- Evaluaciones offline y métricas de calidad.

**Criterio:** toda sugerencia IA es reproducible, auditable y separada de la emisión determinística.

### Fase 7 — Equivalence Lab y release (2 semanas)

- Panel para seleccionar casos COBOL.
- Ejecución baseline/candidate.
- Comparación de salidas, archivos y SQL.
- Triage de divergencias.
- Reporte de readiness y gate de promoción.
- Pruebas E2E web, accesibilidad y rendimiento.

**Criterio:** un usuario puede pasar de fuente a modelo, arquitectura, change set y equivalence run sin salir del workbench.

## División propuesta en Agora

1. `theia-platform-spike`
2. `theia-workbench-shell`
3. `theia-source-explorer`
4. `theia-domain-model-view`
5. `theia-architecture-canvas`
6. `theia-shadow-diff`
7. `theia-ai-agents`
8. `theia-change-set-governance`
9. `theia-equivalence-lab`
10. `theia-release-hardening`

Cada work debe exigir artefacto, evidencia automatizada, revisión y aprobación. La primera vertical debe ser un único programa COBOL pequeño, con modelo, MVC, shadow y equivalence funcionando de punta a punta.

## Riesgos y controles

- **Migración demasiado grande:** mantener el dashboard actual durante las primeras fases.
- **Compatibilidad de extensiones:** usar sólo APIs soportadas por Theia y validar versiones.
- **Complejidad visual:** limitar cada vista a un contexto; evitar grafos gigantes.
- **IA opaca:** mostrar evidencia, confianza, prompt, modelo y change set.
- **Cambios irreversibles:** aprobación explícita, diff y rollback.
- **Rendimiento:** análisis asíncrono, caché por hash y carga incremental.
- **Seguridad:** workspace aislado, permisos por herramienta y allowlist de comandos.

## Orden recomendado

No conviene migrar todo el dashboard de una vez. El primer hito debe ser:

> Abrir un programa COBOL, ver su estructura, proyectar un modelo de negocio, elegir MVC, visualizar el shadow Java, pedir una explicación a un agente IA y ejecutar el harness de equivalencia, todo dentro de Theia.

Si esa vertical funciona, el resto es expansión de vistas y agentes sobre una base validada.
