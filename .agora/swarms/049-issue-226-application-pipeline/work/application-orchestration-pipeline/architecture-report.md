# Informe de arquitectura — AC-05

## Resultado

La implementación establece `renovatio-application` como módulo Java puro y autoridad de
orquestación. `RenovatioApplication` publica los nueve casos de uso acordados y
`DefaultRenovatioApplication` ejecuta un pipeline determinista sobre modelos inmutables.

## Límites

- Los contratos de análisis, propuestas, proyección, emisión, refinamiento, validación,
  persistencia de proyectos/artifacts, Git, red y reloj están definidos como ports del módulo.
- El módulo no depende de Spring, API, CLI, MCP ni providers concretos.
- API, CLI y MCP ejecutan capacidades mediante `ApplicationCommandBus`. El único acceso al
  registry legacy está encapsulado por `LegacyProviderApplicationAdapter`; su retiro corresponde
  a AC-06 y no modifica los contratos externos de AC-05.
- Plan y preview no publican archivos. Preview, validate y apply comparten las identidades
  canónicas de source snapshot y artifact manifest.
- Apply conserva preimage/postimage, valida staleness, publica atómicamente y compensa filesystem
  y Git local ante fallos. La red queda fuera de la transacción.

## Guardas verificadas

`ApplicationModuleBoundaryTest` impide dependencias de framework en application y
`ApplicationAdapterBoundaryTest` impide que las rutas productivas de API, CLI y MCP invoquen
directamente `LanguageProviderRegistry.routeToolCall` o `MigrationPlanService`.

## Deuda explícita

`ApplicationCommandBus` es una frontera de compatibilidad temporal para capacidades legacy. No
duplica orquestación: centraliza el salto hacia providers hasta que AC-06 reemplace el registry con
adaptadores tipados de `RenovatioApplication`.
