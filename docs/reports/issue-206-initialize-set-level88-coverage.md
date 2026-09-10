# Cobertura #206 ciclo 2 — `INITIALIZE` y `SET` level-88

Medición sobre los 44 programas del corpus CardDemo vendorizado, ejecutada el 2026-09-08 con:

```text
mvn -pl renovatio-provider-cobol -am test -Dgroups=coverage -Drenovatio.surefire.excludedGroups= -Dexec.skip=true
```

| Métrica | Base posterior a #215 | Ciclo 2 | Delta |
| --- | ---: | ---: | ---: |
| Parseos correctos | 44/44 | 44/44 | 0 |
| Emisiones Java correctas | 32/44 | 32/44 | 0 |
| Java generado que compila | 11/44 | 11/44 | 0 |
| Acciones manuales | 802 | 749 | **−53 (−6,6%)** |

La mejora elimina residuo de las formas soportadas de `INITIALIZE` y `SET` sin ocultar variantes
no resolubles. Las 12 fallas de emisión restantes se conservan en el reporte canónico y están
dominadas por construcciones fuera de este ciclo y fallas de formateo OpenRewrite sobre Java ya
inválido. El detalle por programa queda en
[`carddemo-coverage.md`](carddemo-coverage.md) y su fuente estructurada en
[`carddemo-coverage.json`](carddemo-coverage.json).
