# Runbook: COBOL to Java Reference Pipeline

## Pre-requisitos
- Java 21 (JDK completo)
- Maven 3.9+
- Git
- Menos de 10 minutos para la validación de referencia desde clone limpio; en una máquina con dependencias Maven en caché, la suite medida tarda ~2 minutos.

## Pasos

### 1. Clone limpio
```bash
git clone https://github.com/Modern-Ash/renovatio.git
cd renovatio
```

### 2. Build del proyecto
```bash
./mvnw clean install -DskipTests
```

### 3. Ejecutar tests del pipeline end-to-end
```bash
# Test de fixtures (verifica que existen todos los archivos requeridos)
./mvnw test -pl renovatio-provider-cobol -Dtest=FixturesExistenceTest

# Test end-to-end del pipeline (ejecuta pipeline completo para cada fixture)
./mvnw test -pl renovatio-provider-cobol -Dtest=PipelineE2ETest
```

### 3b. Ejecutar la demo por CLI
```bash
./mvnw -q -pl renovatio-cli -am -DskipTests package
java -jar renovatio-cli/target/renovatio.jar reference-pipeline \
  renovatio-provider-cobol/src/test/resources/fixtures/batch-simple \
  --out /tmp/renovatio-reference-cli/batch-simple
```

### 3c. Verificar la ruta API
```bash
./mvnw -q -pl renovatio-api -am \
  -Dtest=ReferencePipelineApiTest \
  -Djacoco.skip=true \
  -Dexec.skip=true \
  test
```

### 4. Ejecutar todos los tests del módulo
```bash
./mvnw test -pl renovatio-provider-cobol

# Guardrail de caracterización (incluye los sidecars anotados)
./mvnw test -pl renovatio-provider-cobol -Dtest=CharacterizationFixtureContractTest
```

### 5. Verificar resultados esperados

#### Fixtures creados
Los 3 fixtures están en `renovatio-provider-cobol/src/test/resources/fixtures/`:
- `batch-simple/` - Programa batch con MOVE, COMPUTE, IF/ELSE, DISPLAY
- `cics-mvc/` - Programa CICS con EXEC CICS SEND/RECEIVE
- `db2-access/` - Programa con EXEC SQL SELECT/INSERT/UPDATE

Cada fixture contiene:
- `src/cobol/` - Archivos COBOL fuente
- `decisions.json` - Decisiones de arquitectura
- `expected/` - Salida esperada (Java files)

#### Tests esperados
- `FixturesExistenceTest`: 6 tests pasando
- `PipelineE2ETest`: 12 tests pasando (pipeline productivo, build Maven y repetibilidad)
- `ReferencePipelineApiTest`: endpoint REST del pipeline pasando
- Suite completa medida: 149 tests pasando en `renovatio-provider-cobol`
- Tiempo medido de la suite completa: 88.62 s (JDK 21.0.12, dependencias en caché)

#### Evidencia desde clone limpio
Para reproducir la validación cronometrada desde un clone limpio local:

```bash
rm -rf /tmp/renovatio-reference-clean
git clone file://"$PWD" /tmp/renovatio-reference-clean
cd /tmp/renovatio-reference-clean
/usr/bin/time -p ./mvnw -q \
  -pl renovatio-provider-cobol,renovatio-cli,renovatio-api -am \
  -Dtest=PipelineE2ETest,PipelineValidationTest,SurfaceProofTest,FixturesExistenceTest,RenovatioCliSmokeTest,ReferencePipelineApiTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Djacoco.skip=true \
  -Dexec.skip=true \
  test
```

#### Criterios de aceptación satisfechos
- `fixtures`: Archivos redistribuibles con inputs, decisions, manifests y outputs esperados
- `end-to-end`: Pipeline completo de 10 etapas ejecutándose para cada fixture
- `determinism`: Dos ejecuciones producen outputs byte-identical (salvo metadata excluida)
- `idempotency`: Reaplicar mismo ChangeSet no genera cambios adicionales
- `semantic-gaps`: Statements no soportados generan Action Items estables
- `equivalence`: Comparación contra el conjunto completo de golden Java (faltantes, inesperados y contenido)
- `surface-proof`: La misma ruta está disponible por servicio application, MCP, CLI y API

### 6. Revisar Action Items (semantic gaps)
Los Action Items se generan durante la ejecución del pipeline y se registran en el `PipelineResult.semanticGaps()`.

### 7. Verificar equivalencia
El `EquivalenceChecker` compara el output generado contra los golden files en `expected/`.

### 8. Verificar determinismo
El `DeterminismValidator` ejecuta el pipeline múltiples veces y compara los outputs.

### 9. Verificar idempotencia
El `IdempotencyValidator` verifica que reaplicar el mismo ChangeSet no genera cambios.

## Troubleshooting

### Error: "Build failed"
Verificar que Maven está instalado y JAVA_HOME está configurado.

### Error: "Fixture not found"
Verificar que los archivos COBOL están en la ubicación correcta dentro de `src/test/resources/fixtures/`.

### Error: "Equivalence mismatch"
Revisar el `EquivalenceReport` para ver las divergencias. Las diferencias en timestamps y rutas absolutas son esperadas.

## Métricas de éxito
- Todos los tests pasan (149/149 en el módulo del proveedor)
- Pipeline ejecuta las 10 etapas completas
- Build Maven exitoso para cada fixture
- 0 errores de compilación
- 0 Action Items con `severity=BLOCKING` en las tres fixtures; los casos no traducidos se conservan como warnings estables
