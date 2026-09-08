# Revalidación · #206 ciclo 1 tras integrar `main`

- **Swarm/work:** `issue-206-display-continue-goback` / `display-continue-goback`
- **Commit probado:** `cf6cd90c50707fd44ed4543c96d2dfe5fa1ac7d4`
- **Base integrada:** `d8a3a930529542229ae99defa5c33789f06b78e5` (#217)
- **Fecha:** 2026-09-08

## Resultado

- `mvn -pl renovatio-provider-cobol,cobol-openrewrite-recipes -am test -Dexec.skip=true`
  terminó con **BUILD SUCCESS**; `renovatio-provider-cobol` 114/114 y
  `cobol-openrewrite-recipes` 26/26.
- El comando de cobertura se ejecutó dos veces y produjo los mismos hashes:
  - JSON: `9de03cc7e116dacad081c7d7a28ca652af1dacb9343d25f817d6b7800df42fee`
  - Markdown: `321b98756916fcfe9df5449b35ab35f7e72dfd43e4933f27bece4dec1c58f227`
- Los reportes no contienen rutas `/tmp` ni nombres `carddemo-coverage-javac-*`.
- Se conserva el resultado funcional: 11/44 programas compilan; `DISPLAY`, `CONTINUE`,
  `GOBACK` y `STOP RUN` ya no figuran entre los constructos no soportados.

Este URI versionado conserva inmutables los bytes de la revisión 2 sin alterar el informe
histórico registrado por la revisión 1.
