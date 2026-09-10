# Spec: Build reproducible y CI obligatoria (#223)

## Resumen

Convertir el repositorio Renovatio en un build reproducible desde clone limpio para Java, UI (renovatio-ui), Workbench (renovatio-workbench) y Python, proteger main con gates de CI equivalentes, y documentar una única orden bootstrap que funcione idénticamente local y en CI.

## Problema observado

- POMs malformados: `renovatio-mcp-server` declarado 3 veces en `dependencyManagement`, `renovatio-emitter-node` con plugin JaCoCo sin versión, inconsistencias Java 17/21 entre módulos
- JaCoCo no centralizado en `pluginManagement` raíz — configuración duplicada en 8 módulos hijos
- Sin Maven Wrapper — builds dependen de la versión de Maven instalada en cada máquina
- Colisión Node multiprograma: `docs/node-idioms.md` no existe como artifact compartido
- renovatio-api ejecuta Vite (build de renovatio-ui) sin instalar `node_modules` primero
- Sin configuración CI en el repositorio — ningún gate automático protege main
- Python con dos suites de test separadas sin ejecución automatizada

## Alcance

### Dentro
- Maven Wrapper con versión fija
- Centralización de JaCoCo y dependencias en root `pom.xml`
- Resolución de duplicados y versiones inconsistentes
- Script de bootstrap único que cubre Java + Node UI + Node Workbench + Python
- GitHub Actions CI matrix para todos los componentes
- Documentación de build en README

### Fuera
- Migración de frameworks (no cambiar Spring Boot, Theia, Vite)
- Actualización de dependencias por conveniencia (sólo las necesarias para reproducibilidad)
- Ocultar tests con skip/continue-on-error sin decisión documentada
- Evidencia producida con targets o node_modules heredados

## Acceptance Criteria Detallados

### AC: single-bootstrap

**Definición:** Un clone limpio ejecuta un solo comando que instala toolchains, dependencias bloqueadas por lockfile y ejecuta el build completo sin estado previo de `node_modules`.

**Implementación:**
1. Instalar Maven Wrapper 3.9.6 (última estable) con `.mvn/wrapper/maven-wrapper.properties`
2. Script `scripts/bootstrap.sh` que:
   - Verifica Java 21+ (usa `JAVA_HOME` o detecta del PATH)
   - Ejecuta `./mvnw clean install` (compila Java + empaqueta todo)
   - `cd renovatio-ui && npm ci && npm run build` (build Vite → output a `renovatio-api/src/main/resources/static/`)
   - `cd renovatio-workbench && npm ci && npm run build` (build Theia)
   - `cd renovatio-provider-python && pip install -e ".[test]"` o equivalente con venv
   - Ejecuta tests de cada componente
3. El script debe fallar en el primer error (sin `|| true`)
4. Script idéntico invocado por CI y por developers

**Evidencia:** Output de `scripts/bootstrap.sh` ejecutado en clone limpio temporal, sin `node_modules` ni `target/` previos.

### AC: maven-green

**Definición:** El reactor Maven completo termina verde (`mvnw clean install` sin errores).

**Implementación:**
1. Remover las 2 entradas duplicadas de `renovatio-mcp-server` en `dependencyManagement` (quedar 1 sola)
2. Agregar `jacoco-maven-plugin` versión `0.8.10` a `<pluginManagement>` raíz
3. Agregar `renovatio-cli`, `renovatio-cobol-runtime`, `renovatio-evals` a `<dependencyManagement>` raíz
4. Eliminar versiones hardcoded en `renovatio-provider-java` (usar `${openrewrite.version}` y `${jgit.version}`)
5. Arreglar `renovatio-emitter-node`: agregar versión JaCoCo, mover regla de check al execution correcto
6. Remover declaraciones redundantes de `${project.version}` en módulos persistence y provider-java
7. Remover `<version>3.11.0</version>` redundante de maven-compiler-plugin en shared
8. Decidido: unificar todos los módulos a Java 21 (shared, core, provider-java migran de 17 a 21)

**Evidencia:** `./mvnw clean install` output completo, 0 errores, todos los tests pasan.

### AC: node-collision

**Definición:** La generación Node multiprograma no produce paths duplicados para `docs/node-idioms.md` y conserva determinismo del manifest.

**Implementación:**
1. `docs/node-idioms.md` es un artifact generado por el build del emitter-node, no un archivo committed. Se genera en `target/` o directorio de output, no en `docs/`
2. Si el emitter-node genera este archivo, asegurar que el output sea determinístico (mismo input → mismo output)
3. Agregar validación en el build o test que verifique que no existen paths duplicados en el manifest del emitter-node
4. Si el problema es el generador de paths multiprograma, fixear la lógica de generación para usar paths únicos

**Evidencia:** Test que reproduce la colisión, la corrige, y verifica que el manifest es determinístico.

### AC: versions-pinned

**Definición:** Java, Maven plugins (incluido JaCoCo), Node/package manager y Python están fijados o validados explícitamente.

**Implementación:**
| Toolchain | Pinning | Mecanismo |
|-----------|---------|-----------|
| Java | 21 | `.java-version` o validación en `scripts/bootstrap.sh` |
| Maven | 3.9.6 | `.mvn/wrapper/maven-wrapper.properties` → `distributionUrl` |
| Maven Compiler Plugin | 3.11.0 | Root `pluginManagement` |
| JaCoCo | 0.8.10 | Root `pluginManagement` |
| Node | 24.20.0 | `renovatio-workbench/.nvmrc` (ya existe), agregar `.nvmrc` a `renovatio-ui` |
| npm | 11.19.0 | `renovatio-workbench/package.json` `packageManager` (ya existe) |
| Python | 3.10+ | `pyproject.toml` `requires-python` (ya existe) |
| pip | latest stable | `scripts/bootstrap.sh` |

**Evidencia:** Cada versión declarada en un solo lugar, verificable por el script de bootstrap.

### AC: ci-matrix

**Definición:** CI ejecuta Maven, renovatio-ui, renovatio-workbench, Python, caracterización y equivalencia, con fallos de seguridad relevantes no tolerados.

**Implementación:**
GitHub Actions workflow `.github/workflows/ci.yml` con jobs:

```yaml
jobs:
  java:
    runs-on: ubuntu-latest
    steps: setup-java-21, setup-maven-wrapper, mvnw clean install
  renovatio-ui:
    runs-on: ubuntu-latest
    needs: java
    steps: setup-node-24, npm ci, npm run build, npm test
  renovatio-workbench:
    runs-on: ubuntu-latest
    needs: java
    steps: setup-node-24, npm ci, npm run build, npm test
  python:
    runs-on: ubuntu-latest
    steps: setup-python-3.12, pip install, pytest
  characterization:
    runs-on: ubuntu-latest
    needs: java
    steps: ejecutar tests de caracterización COBOL
  equivalence:
    runs-on: ubuntu-latest
    needs: java
    steps: ejecutar tests de equivalencia
```

- Todos los jobs son required checks para merge a main
- Ningún job usa `continue-on-error` o `skip` sin decisión documentada
- Caches de Maven/Node como optimización, no como fallback

**Evidencia:** Workflow YAML committed, exec exitoso en CI, checks verificados como required.

### AC: clean-clone-proof

**Definición:** La evidencia proviene de un clone temporal limpio sin targets ni `node_modules` heredados.

**Implementación:**
1. Workflow de CI ejecuta en `ubuntu-latest` sin cache previa en primer run
2. Script `scripts/verify-clean-build.sh` que:
   - Crea directorio temporal
   - `git clone` el repo
   - Ejecuta `scripts/bootstrap.sh`
   - Verifica que no hay `node_modules` fuera de los esperados
   - Verifica que `target/` y `node_modules/` se generaron durante el build
3. Evidencia documentada con: comando exacto, entorno, commit SHA, resultado

**Evidencia:** Log de ejecución en directorio temporal limpio.

### AC: developer-parity

**Definición:** Las mismas órdenes funcionan localmente y en CI, documentadas en README.

**Implementación:**
1. `scripts/bootstrap.sh` es la única comando necesaria (local y CI)
2. README actualizado con:
   - Prerrequisitos (Java 21, Node 24, Python 3.10+)
   - `git clone <repo> && cd renovatio && ./scripts/bootstrap.sh`
   - Qué hace cada paso del script
   - Cómo ejecutar componentes individuales
3. CI invoca exactamente `scripts/bootstrap.sh` (no comandos paralelos)

**Evidencia:** README actualizado, CI ejecuta el mismo script.

## Artefactos requeridos

- `spec` (este documento)
- `implementation-plan` (pasos de implementación con dependencias)
- `build-manifest` (versiones fijadas de todas las toolchains)
- `ci-report` (evidencia de CI matrix ejecutándose)
- `test-report` (evidencia de tests pasando desde clone limpio)

## Dependencias

- Depende de AC-01 (architecture assessment completado)
- Bloquea AC-07 y AC-11 (sus contratos de CI serán consumidos)

## Definición de Done

El work se cierra cuando:
- Todos los ACs están en estado `accepted`
- `scripts/bootstrap.sh` funciona en clone limpio
- CI matrix está configurada y passing
- README documenta el proceso completo
- Evidencia de clean-clone-build está registrada como artifact Agora
