# Verificación #206 ciclo 2 — `INITIALIZE` y `SET` level-88

Fecha: 2026-09-08

## Resultado

Todas las verificaciones finalizaron con código 0:

- Parseo e IR tipado: formas soportadas, fallback explícito, validación e inmutabilidad.
- Resolución semántica: defaults PIC, valores level-88 `TRUE/FALSE` y dominio numérico sin signo.
- Caracterización: fixture nuevo genera, compila y ejecuta el Java esperado sin acciones manuales.
- Guardrails: residuos desconocidos generan exactamente dos acciones manuales y son estables entre
  ejecuciones.
- Esquemas: los dos nodos nuevos validan contra `cobol-ir.v1` y participan en el IR anotado.
- Cobertura CardDemo: 44/44 parseos, 32/44 emisiones, 11/44 compilaciones y 749 acciones manuales.

## Comandos

```text
mvn -pl cobol-openrewrite-recipes,renovatio-provider-cobol -am -Dtest=CobolDataVerbValueResolverTest,PopulateCobolProcessRecipeTest,JavaGenerationServiceTest,CharacterizationFixtureContractTest,GuardrailSchemaCatalogTest -Dsurefire.failIfNoSpecifiedTests=false -Dexec.skip=true test

mvn -pl renovatio-provider-cobol -am test -Dgroups=coverage -Drenovatio.surefire.excludedGroups= -Dexec.skip=true

mvn -pl renovatio-cobol-ir,cobol-openrewrite-recipes,renovatio-provider-cobol -am test -Dexec.skip=true

git diff --check
```

La primera regresión detectó correctamente una expectativa antigua de ocho variantes del esquema;
se actualizó a diez y ahora afirma explícitamente las referencias de `initializeStatement` y
`setConditionStatement`.
