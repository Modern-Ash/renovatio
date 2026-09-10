# Revalidación de reproducibilidad · #217

- **Swarm/work:** `issue-217-carddemo-coverage` / `carddemo-coverage-report`
- **Commit probado:** `cf6cd90c50707fd44ed4543c96d2dfe5fa1ac7d4`
- **Fecha:** 2026-09-08

## Corrección

La primera implementación era funcional, pero los errores de `javac` conservaban el nombre
aleatorio del directorio `@TempDir`. Eso hacía variar el JSON y el Markdown entre corridas.
La revisión 2 normaliza únicamente ese prefijo en
`CardDemoCoverageReportTest.stableCompilerOutput`.

## Evidencia

Dos ejecuciones consecutivas del comando documentado produjeron exactamente:

- JSON: `9de03cc7e116dacad081c7d7a28ca652af1dacb9343d25f817d6b7800df42fee`
- Markdown: `321b98756916fcfe9df5449b35ab35f7e72dfd43e4933f27bece4dec1c58f227`

No queda ninguna ruta `/tmp` ni nombre `carddemo-coverage-javac-*`. El reactor afectado terminó
verde: `renovatio-provider-cobol` 114/114 y `cobol-openrewrite-recipes` 26/26.

El arreglo atribuible a #217 sólo modifica la herramienta de cobertura y sus reportes. La rama de
integración contiene además los cambios de traductor de #206, gobernados y verificados por su
propio trabajo; no forman parte del cambio correctivo de #217.
