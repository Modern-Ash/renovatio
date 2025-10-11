---
# Metadatos de la especificación
title: "Migración de programa COBOL básico a Java con Spring Boot"
status: "ejemplo"
version: "1.0.0"
author: "equipo-renovatio"
reviewers: ["arquitecto-java", "especialista-cobol"]
priority: "medium"
labels: ["cobol", "java", "migration", "spring-boot", "ejemplo"]
linked_issues: []
target_modules: ["renovatio-provider-cobol", "renovatio-core"]

# Metadatos específicos de Renovatio
renovatio:
  source_language: "cobol"
  target_language: "java"
  complexity: "básica"
  estimated_duration: "1-2 semanas"
  modules:
    - "renovatio-provider-cobol"
    - "renovatio-core"
  tools_sequence:
    - name: "cobol.analyze"
      params:
        workspacePath: "${COBOL_WORKSPACE}"
        scope: "programs"
    - name: "cobol.metrics"
      params:
        workspacePath: "${COBOL_WORKSPACE}"
    - name: "cobol.plan"
      params:
        workspacePath: "${COBOL_WORKSPACE}"
        targetLanguage: "java"
    - name: "cobol.apply"
      params:
        planId: "${PLAN_ID}"
        dryRun: false
        outputPath: "${JAVA_OUTPUT}"
  expected_outcomes:
    files_analyzed: "10-15"
    files_migrated: "10-15"
    lines_of_code: "~2000"
    success_rate: ">= 95%"
---

# Migración de programa COBOL básico a Java con Spring Boot

## 1. Resumen ejecutivo

### Contexto
Una aplicación mainframe con programas COBOL que realizan operaciones CRUD sobre archivos secuenciales necesita ser modernizada para ejecutarse en un entorno cloud con Java y Spring Boot.

### Estado actual
- 15 programas COBOL con lógica de negocio básica
- Sin SQL embebido (solo archivos planos)
- Sin integraciones con CICS o DB2
- Lógica procedural con PERFORM y GO TO
- ~2000 líneas de código total

### Resultado esperado
- Código Java equivalente usando Spring Boot
- Servicios REST para operaciones CRUD
- Persistencia con archivos o base de datos simple
- Tests unitarios generados automáticamente
- 100% de la lógica migrada

## 2. Objetivos y métricas

### Objetivos

1. **Migrar lógica COBOL a Java**
   - Convertir procedimientos COBOL en métodos Java
   - Mantener la semántica original
   - Generar código legible y mantenible

2. **Generar estructura Spring Boot**
   - Controllers para endpoints REST
   - Services para lógica de negocio
   - Repositories para acceso a datos
   - DTOs para transferencia de datos

3. **Automatizar con Renovatio MCP**
   - Usar herramientas MCP para el proceso completo
   - Generar código sin intervención manual
   - Validar resultados automáticamente

### Métricas clave

| Métrica | Objetivo | Medición |
|---------|----------|----------|
| Programas analizados | 15 | `cobol.analyze` |
| Programas migrados | 15 (100%) | `cobol.apply` |
| Errores de compilación | 0 | `mvn compile` |
| Cobertura de tests | ≥ 70% | `mvn verify` |
| Tiempo total | ≤ 2 semanas | Calendario |

## 3. Alcance técnico

### Módulos involucrados

- **renovatio-core**: Orquestación de la migración
- **renovatio-provider-cobol**: Parser COBOL, generador Java, templates
- **renovatio-shared**: Modelos y DTOs compartidos
- **Salida**: Proyecto Spring Boot independiente

### APIs MCP / herramientas utilizadas

1. `cobol.analyze` - Análisis estático de programas COBOL
2. `cobol.metrics` - Cálculo de métricas de código
3. `cobol.plan` - Generación de plan de migración
4. `cobol.apply` - Aplicación del plan (generación de código Java)
5. `cobol.diff` - Comparación de cambios

## 4. Diseño y plan de ejecución

### Fase 1: Preparación (1 día)
- Configurar workspace COBOL
- Verificar sintaxis de programas
- Preparar entorno Renovatio

### Fase 2: Análisis (1-2 días)
- Ejecutar análisis estático
- Revisar métricas
- Identificar casos especiales

### Fase 3: Planificación (1 día)
- Generar plan de migración
- Revisar estrategia de conversión

### Fase 4: Ejecución (2-3 días)
- Dry-run de la migración
- Migración productiva
- Generación de tests

### Fase 5: Validación (2-3 días)
- Pruebas funcionales
- Pruebas de rendimiento
- Revisión de código

## 5. Riesgos y consideraciones

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Lógica COBOL ambigua | Alto | Revisión manual, tests exhaustivos |
| Parser COBOL falla | Alto | Validar sintaxis antes |
| Código Java no compila | Medio | Dry-run, ajuste de templates |

## 6. Validación y pruebas

### Automatizadas
```bash
mvn clean compile    # Compilación
mvn test             # Tests unitarios
mvn verify           # Verificación + coverage
```

### Manuales
- Validar endpoints REST
- Comparar comportamiento COBOL vs Java
- Probar casos límite

## 7. Checklist de implementación

- [ ] Preparar workspace COBOL
- [ ] Ejecutar `cobol.analyze`
- [ ] Ejecutar `cobol.metrics`
- [ ] Ejecutar `cobol.plan`
- [ ] Ejecutar `cobol.apply` (dry-run)
- [ ] Ejecutar `cobol.apply` (producción)
- [ ] Compilar código Java
- [ ] Ejecutar tests
- [ ] Validación funcional
- [ ] Documentar resultados

## 8. Seguimiento y comunicación

**Stakeholders**: Product Owner, Arquitecto, Especialista COBOL, QA

**Comunicación**: Kickoff, daily updates, demo final, retrospectiva

## 9. Apéndice

### Ejemplo de mapeo COBOL → Java

**COBOL:**
```cobol
01  CUSTOMER-RECORD.
    05  CUSTOMER-ID    PIC 9(8).
    05  CUSTOMER-NAME  PIC X(50).
```

**Java:**
```java
@Entity
public class Customer {
    @Id
    private Long customerId;
    private String customerName;
    // getters, setters
}
```

---

**Este es un ejemplo de especificación siguiendo el formato Spec Kit + Renovatio**
