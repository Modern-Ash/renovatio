# Informe de pruebas — AC-05

## Suite del pipeline

`DefaultRenovatioApplicationTest` y el test kit reusable cubren:

- los nueve casos de uso públicos;
- determinismo de plan y preview y paridad de manifest;
- ausencia de escrituras en plan y preview;
- replay idempotente y conflicto por reutilización de key;
- rechazo de source/manifest obsoleto;
- ChangeSet con preimage y postimage;
- rollback mediante fault injection sin artifacts parciales.

## Integración de adaptadores

- `LegacyProviderApplicationAdapterTest` verifica delegación única, validación de capability y
  copia defensiva de argumentos.
- `ApplicationAdapterBoundaryTest` verifica la frontera productiva común de API, CLI y MCP.
- `FullJobLifecycleTest` verifica plan/apply/diff vía API, incluyendo `runId`, archivos modificados
  y preservación de `outputDir`.

## Comandos ejecutados

```text
./mvnw -pl renovatio-api -am -Dtest=FullJobLifecycleTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -pl renovatio-application,renovatio-core,renovatio-cli,renovatio-mcp-server,renovatio-api -am test
```

Ambos comandos finalizaron con `BUILD SUCCESS`, sin fallos ni errores. El reactor conjunto validó
21 módulos; los módulos directamente modificados reportaron application 8, core 42, MCP 22, CLI
25 y API 63 pruebas exitosas.

La suite MCP intentó conexiones opcionales a servidores locales no disponibles en el sandbox;
esas comprobaciones no ejecutaron tests externos y el conjunto local MCP terminó con 22 pruebas
exitosas.
