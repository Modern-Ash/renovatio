# Runbook: COBOL to Java Reference Pipeline

## Pre-requisitos
- Java 17+
- Maven 3.9+
- Git
- 10 minutos máximo

## Pasos

### 1. Clone limpio
```bash
git clone https://github.com/Modern-Ash/renovatio.git
cd renovatio
```

### 2. Build del proyecto
```bash
mvn clean install -DskipTests
```

### 3. Ejecutar tests del pipeline end-to-end
```bash
# Test de fixtures (verifica que existen todos los archivos requeridos)
mvn test -pl renovatio-provider-cobol -Dtest=FixturesExistenceTest

# Test end-to-end del pipeline (ejecuta pipeline completo para cada fixture)
mvn test -pl renovatio-provider-cobol -Dtest=PipelineE2ETest
```

### 4. Ejecutar todos los tests del módulo
```bash
mvn test -pl renovatio-provider-cobol
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
- `FixturesExistenceTest`: 6/6 tests pasando
- `PipelineE2ETest`: 7/7 tests pasando (pipeline completo + build Maven)
- Total: 46 tests pasando en el módulo

#### Criterios de aceptación satisfechos
- `fixtures`: Archivos redistribuibles con inputs, decisions, manifests y outputs esperados
- `end-to-end`: Pipeline completo de 7 etapas ejecutándose para cada fixture
- `determinism`: Dos ejecuciones producen outputs byte-identical (salvo metadata excluida)
- `idempotency`: Reaplicar mismo ChangeSet no genera cambios adicionales
- `semantic-gaps`: Statements no soportados generan Action Items estables
- `equivalence`: Comparación contra golden files con tolerancias declaradas

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
- Todos los tests pasan (46/46)
- Pipeline ejecuta las 7 etapas completas
- Build Maven exitoso para cada fixture
- 0 errores de compilación
- 0 Action Items con severity=BLOCKING
