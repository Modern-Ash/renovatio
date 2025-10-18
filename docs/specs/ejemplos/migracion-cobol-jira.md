---
# Metadatos de la especificación
title: "Migración de COBOL con DB2 embebido a Java Spring Boot + JPA"
status: "in-progress"
version: "1.1.0"
author: "equipo-renovatio"
reviewers: ["arquitecto-java", "especialista-cobol", "dba-senior"]
priority: "high"
labels: ["cobol", "java", "migration", "db2", "jpa", "spring-boot"]
target_modules: ["renovatio-provider-cobol", "renovatio-core"]

# Integración Jira
jira_epic: "RENO-100"
jira_parent_story: "RENO-101"
linked_jira_issues: ["RENO-102", "RENO-103", "RENO-104", "RENO-105", "RENO-106"]
jira_project: "RENO"
jira_sprint: "Sprint 5 & 6"
jira_labels: ["migration", "cobol-to-java", "db2", "jpa", "critical"]

# Integración GitHub
linked_github_issues: [150, 151, 152]
github_milestone: "v2.1.0"
sync_enabled: true

# Metadatos específicos de Renovatio
renovatio:
  source_language: "cobol"
  target_language: "java"
  complexity: "alta"
  estimated_duration: "3-4 semanas"
  modules:
    - "renovatio-provider-cobol"
    - "renovatio-core"
    - "renovatio-shared"
  tools_sequence:
    - name: "cobol.analyze"
      params:
        workspacePath: "${COBOL_WORKSPACE}"
        scope: "programs-with-db2"
    - name: "cobol.metrics"
      params:
        workspacePath: "${COBOL_WORKSPACE}"
    - name: "cobol.migrate_db2"
      params:
        workspacePath: "${COBOL_WORKSPACE}"
        programPath: "CUSTPROG.cbl"
        outputPath: "${JAVA_OUTPUT}/jpa"
    - name: "cobol.plan"
      params:
        workspacePath: "${COBOL_WORKSPACE}"
        targetLanguage: "java"
        includeDb2Migration: true
    - name: "cobol.apply"
      params:
        planId: "${PLAN_ID}"
        dryRun: false
        outputPath: "${JAVA_OUTPUT}"
  expected_outcomes:
    programs_analyzed: "8"
    programs_migrated: "8"
    db2_statements_converted: "~50"
    jpa_entities_generated: "15"
    lines_of_code: "~5000"
    success_rate: ">= 90%"
---

# Migración de COBOL con DB2 embebido a Java Spring Boot + JPA

> **Jira Epic**: [RENO-100: Modernización módulo Customer Management](https://your-company.atlassian.net/browse/RENO-100)  
> **GitHub Issue**: [#150](https://github.com/accentureshark/renovatio/issues/150)  
> **Sprint**: Sprint 5 & 6 (Oct 2025)

## 1. Resumen ejecutivo

### Contexto
El módulo Customer Management del sistema legacy mainframe está escrito en COBOL y utiliza DB2 embebido mediante sentencias EXEC SQL. Este módulo gestiona operaciones CRUD sobre clientes, direcciones y transacciones, siendo crítico para el negocio.

**Jira Story Principal**: [RENO-101 - Análisis y planificación de migración](https://your-company.atlassian.net/browse/RENO-101)

### Estado actual
- 8 programas COBOL con lógica de negocio compleja
- ~50 sentencias EXEC SQL embebidas (SELECT, INSERT, UPDATE, DELETE)
- 15 tablas DB2 involucradas
- Sin CICS (standalone batch programs)
- ~5000 líneas de código COBOL
- Documentación parcial

### Resultado esperado
- Código Java equivalente usando Spring Boot 3.x
- Entidades JPA mapeadas desde las tablas DB2
- Servicios REST para operaciones CRUD
- Repositorios Spring Data JPA
- Tests unitarios y de integración generados automáticamente
- Documentación actualizada con ejemplos
- 100% de la funcionalidad migrada

## 2. Objetivos y métricas

### Objetivos principales

1. **Migrar programas COBOL a Java** ([RENO-102](https://your-company.atlassian.net/browse/RENO-102))
   - Convertir procedimientos COBOL en clases y métodos Java
   - Mantener la lógica de negocio intacta
   - Generar código Java idiomático y mantenible

2. **Convertir DB2 embebido a JPA** ([RENO-103](https://your-company.atlassian.net/browse/RENO-103))
   - Mapear tablas DB2 a entidades JPA
   - Traducir EXEC SQL a consultas JPQL/Criteria API
   - Implementar transacciones con @Transactional

3. **Generar estructura Spring Boot** ([RENO-104](https://your-company.atlassian.net/browse/RENO-104))
   - Controllers REST para endpoints
   - Services para lógica de negocio
   - Repositories JPA para persistencia
   - DTOs con validación

4. **Automatizar con Renovatio MCP** ([RENO-105](https://your-company.atlassian.net/browse/RENO-105))
   - Proceso end-to-end sin intervención manual
   - Validación automática de resultados
   - Generación de tests

5. **Documentar y transferir conocimiento** ([RENO-106](https://your-company.atlassian.net/browse/RENO-106))
   - Guías de migración
   - Mapeos COBOL → Java documentados
   - Ejemplos de uso

### Métricas clave

| Métrica | Objetivo | Medición | Jira Tracking |
|---------|----------|----------|---------------|
| Programas analizados | 8 (100%) | `cobol.analyze` | [RENO-102](https://jira/RENO-102) |
| Programas migrados | 8 (100%) | `cobol.apply` | [RENO-103](https://jira/RENO-103) |
| Sentencias SQL convertidas | ~50 (100%) | `cobol.migrate_db2` | [RENO-103](https://jira/RENO-103) |
| Entidades JPA generadas | 15 | Manual count | [RENO-104](https://jira/RENO-104) |
| Errores de compilación | 0 | `mvn compile` | [RENO-105](https://jira/RENO-105) |
| Cobertura de tests | ≥ 80% | `mvn verify` | [RENO-105](https://jira/RENO-105) |
| Tests end-to-end pasando | 100% | `mvn test` | [RENO-105](https://jira/RENO-105) |
| Documentación completada | 100% | Manual review | [RENO-106](https://jira/RENO-106) |
| Tiempo total | ≤ 4 semanas | Calendario | [RENO-100](https://jira/RENO-100) |

## 3. Alcance técnico

### Módulos involucrados

- **renovatio-core**: Orquestación de la migración y pipeline general
- **renovatio-provider-cobol**: Parser COBOL, análisis DB2, generador Java/JPA
- **renovatio-shared**: Modelos, DTOs, utilidades de mapeo
- **Salida**: Proyecto Spring Boot 3.x standalone

### APIs MCP / herramientas utilizadas

1. **cobol.analyze** - Análisis estático de programas COBOL con DB2
2. **cobol.metrics** - Métricas de código (LOC, complejidad, SQL statements)
3. **cobol.migrate_db2** - Conversión específica de EXEC SQL a JPA
4. **cobol.plan** - Plan de migración detallado
5. **cobol.apply** - Aplicación del plan (generación de código Java/JPA)
6. **cobol.diff** - Comparación de cambios
7. **java.format** - Formateo del código generado
8. **java.test** - Ejecución de tests generados

### Artefactos generados

- Entidades JPA (`@Entity`, `@Table`, `@Column`)
- Repositorios Spring Data JPA
- Services con lógica de negocio
- Controllers REST con OpenAPI docs
- DTOs con validación Bean Validation
- Tests unitarios con JUnit 5 + Mockito
- Tests de integración con Testcontainers
- Configuración Spring Boot (`application.yml`)

## 4. Diseño y plan de ejecución

### Tareas planificadas (Jira Integration)

#### Sprint 5 (2 semanas)

| Tarea | Jira Ticket | Asignado | Story Points | Estado | GitHub PR |
|-------|-------------|----------|--------------|--------|-----------|
| Análisis de programas COBOL | [RENO-110](https://jira/RENO-110) | @juan-dev | 3 | ✅ Done | [#155](https://github.com/accentureshark/renovatio/pull/155) |
| Análisis de estructura DB2 | [RENO-111](https://jira/RENO-111) | @maria-dba | 2 | ✅ Done | [#156](https://github.com/accentureshark/renovatio/pull/156) |
| Configuración entorno migración | [RENO-112](https://jira/RENO-112) | @carlos-devops | 2 | ✅ Done | [#157](https://github.com/accentureshark/renovatio/pull/157) |
| Parser EXEC SQL mejorado | [RENO-113](https://jira/RENO-113) | @ana-dev | 5 | 🔄 In Progress | [#158 (draft)](https://github.com/accentureshark/renovatio/pull/158) |
| Generador entidades JPA | [RENO-114](https://jira/RENO-114) | @pedro-dev | 8 | 🔄 In Progress | [#159 (draft)](https://github.com/accentureshark/renovatio/pull/159) |

#### Sprint 6 (2 semanas)

| Tarea | Jira Ticket | Asignado | Story Points | Estado | GitHub PR |
|-------|-------------|----------|--------------|--------|-----------|
| Migración programa CUSTPROG | [RENO-115](https://jira/RENO-115) | @juan-dev | 5 | 📋 To Do | - |
| Migración programa ADDRPROG | [RENO-116](https://jira/RENO-116) | @juan-dev | 3 | 📋 To Do | - |
| Migración programas restantes | [RENO-117](https://jira/RENO-117) | @ana-dev | 8 | 📋 To Do | - |
| Tests unitarios e integración | [RENO-118](https://jira/RENO-118) | @laura-qa | 5 | 📋 To Do | - |
| Validación funcional end-to-end | [RENO-119](https://jira/RENO-119) | @laura-qa | 3 | 📋 To Do | - |
| Documentación técnica | [RENO-120](https://jira/RENO-120) | @sofia-tech-writer | 2 | 📋 To Do | - |

### Detalles del plan de ejecución

#### Fase 1: Preparación y análisis (Sprint 5, Semana 1)

**1.1 Análisis de programas COBOL**
- **Jira**: [RENO-110](https://your-company.atlassian.net/browse/RENO-110)
- **GitHub PR**: [#155](https://github.com/accentureshark/renovatio/pull/155) ✅ Merged
- **Descripción**: Ejecutar `cobol.analyze` para identificar patrones, dependencias y complejidad
- **Responsable**: @juan-dev
- **Duración**: 2 días
- **Entregables**: Reporte de análisis con métricas
- **Smart commit ejemplo**: 
  ```bash
  git commit -m "RENO-110 #time 6h #comment Análisis completado, 8 programas identificados con 50 EXEC SQL #close"
  ```

**1.2 Análisis de estructura DB2**
- **Jira**: [RENO-111](https://your-company.atlassian.net/browse/RENO-111)
- **GitHub PR**: [#156](https://github.com/accentureshark/renovatio/pull/156) ✅ Merged
- **Descripción**: Documentar esquema DB2, relaciones, índices y constraints
- **Responsable**: @maria-dba
- **Duración**: 1.5 días
- **Entregables**: Diagrama ER y DDL scripts
- **Criterios de aceptación**: Definidos en ticket Jira

**1.3 Configuración entorno de migración**
- **Jira**: [RENO-112](https://your-company.atlassian.net/browse/RENO-112)
- **GitHub PR**: [#157](https://github.com/accentureshark/renovatio/pull/157) ✅ Merged
- **Descripción**: Preparar workspace, herramientas Renovatio, DB2 de pruebas
- **Responsable**: @carlos-devops
- **Duración**: 1.5 días

#### Fase 2: Desarrollo de herramientas (Sprint 5, Semana 2)

**2.1 Parser EXEC SQL mejorado**
- **Jira**: [RENO-113](https://your-company.atlassian.net/browse/RENO-113)
- **GitHub PR**: [#158 (draft)](https://github.com/accentureshark/renovatio/pull/158) 🔄
- **Descripción**: Mejorar parser para manejar EXEC SQL complejos (cursores, host variables)
- **Responsable**: @ana-dev
- **Duración**: 4 días
- **Dependencias**: RENO-110, RENO-111
- **Progress tracking**:
  ```bash
  git commit -m "RENO-113 #start Iniciando desarrollo del parser"
  git commit -m "RENO-113 #time 3h #comment Implementado parsing de SELECT con host variables"
  git commit -m "RENO-113 #time 2h #comment Añadido soporte para cursores DECLARE/OPEN/FETCH/CLOSE"
  ```

**2.2 Generador de entidades JPA**
- **Jira**: [RENO-114](https://your-company.atlassian.net/browse/RENO-114)
- **GitHub PR**: [#159 (draft)](https://github.com/accentureshark/renovatio/pull/159) 🔄
- **Descripción**: Template Freemarker para generar entidades JPA desde DDL y copybooks
- **Responsable**: @pedro-dev
- **Duración**: 5 días
- **Dependencias**: RENO-111

#### Fase 3: Migración de programas (Sprint 6, Semana 1)

**3.1 Migración de CUSTPROG**
- **Jira**: [RENO-115](https://your-company.atlassian.net/browse/RENO-115)
- **Descripción**: Migrar programa principal de gestión de clientes
- **Responsable**: @juan-dev
- **Duración**: 3 días
- **Dependencias**: RENO-113, RENO-114

**3.2 Migración de ADDRPROG**
- **Jira**: [RENO-116](https://your-company.atlassian.net/browse/RENO-116)
- **Descripción**: Migrar programa de gestión de direcciones
- **Responsable**: @juan-dev
- **Duración**: 2 días
- **Dependencias**: RENO-115

**3.3 Migración de programas restantes**
- **Jira**: [RENO-117](https://your-company.atlassian.net/browse/RENO-117)
- **Descripción**: Migrar 6 programas adicionales (batch jobs, reports)
- **Responsable**: @ana-dev
- **Duración**: 4 días
- **Dependencias**: RENO-116

#### Fase 4: Testing y validación (Sprint 6, Semana 2)

**4.1 Tests unitarios e integración**
- **Jira**: [RENO-118](https://your-company.atlassian.net/browse/RENO-118)
- **Descripción**: Crear tests para todas las clases generadas
- **Responsable**: @laura-qa
- **Duración**: 3 días
- **Meta**: 80%+ coverage

**4.2 Validación funcional end-to-end**
- **Jira**: [RENO-119](https://your-company.atlassian.net/browse/RENO-119)
- **Descripción**: Comparar outputs COBOL vs Java con datos reales
- **Responsable**: @laura-qa
- **Duración**: 2 días

**4.3 Documentación técnica**
- **Jira**: [RENO-120](https://your-company.atlassian.net/browse/RENO-120)
- **Descripción**: Guías de deployment, arquitectura, mapeos COBOL-Java
- **Responsable**: @sofia-tech-writer
- **Duración**: 2 días

## 5. Riesgos y consideraciones

| Riesgo | Impacto | Probabilidad | Mitigación | Jira |
|--------|---------|--------------|------------|------|
| Lógica SQL compleja no migra correctamente | Alto | Media | Revisión manual, tests exhaustivos | [RENO-130](https://jira/RENO-130) |
| Cursores DB2 con comportamiento específico | Alto | Alta | Documentar casos especiales, mapeo manual | [RENO-131](https://jira/RENO-131) |
| Transacciones distribuidas no soportadas | Medio | Baja | Diseñar alternativas con Spring @Transactional | [RENO-132](https://jira/RENO-132) |
| Rendimiento JPA inferior a DB2 nativo | Medio | Media | Optimizar queries, usar caching | [RENO-133](https://jira/RENO-133) |
| Código Java generado no compila | Alto | Baja | Dry-run previo, ajuste de templates | [RENO-134](https://jira/RENO-134) |

## 6. Validación y pruebas

### Automatizadas

```bash
# Compilación
mvn clean compile

# Tests unitarios
mvn test

# Tests de integración con Testcontainers (DB2)
mvn integration-test

# Coverage
mvn verify
mvn jacoco:report

# Tests end-to-end
mvn test -Dtest=CustomerManagementE2ETest
```

**Criterios de aceptación (Jira)**:
- Todos los tests pasan: [RENO-118](https://jira/RENO-118)
- Coverage ≥ 80%: [RENO-118](https://jira/RENO-118)
- 0 errores de compilación: [RENO-105](https://jira/RENO-105)

### Manuales

- Validar endpoints REST con Postman
- Comparar resultados COBOL vs Java
- Probar casos límite (nulls, duplicados, errores)
- Validar transacciones (commit/rollback)

**Tracking**: [RENO-119](https://your-company.atlassian.net/browse/RENO-119)

### Métricas de calidad

| Métrica | Target | Actual | Jira |
|---------|--------|--------|------|
| Test coverage | ≥ 80% | TBD | [RENO-118](https://jira/RENO-118) |
| Bugs críticos | 0 | TBD | [RENO-119](https://jira/RENO-119) |
| Code smells (SonarQube) | < 50 | TBD | [RENO-121](https://jira/RENO-121) |
| Technical debt | < 5 days | TBD | [RENO-121](https://jira/RENO-121) |

## 7. Checklist de implementación

### Planning & Tracking (Jira)
- [x] Jira Epic creada: [RENO-100](https://jira/RENO-100)
- [x] Story principal: [RENO-101](https://jira/RENO-101)
- [x] Sub-tareas creadas para Sprint 5: RENO-110 a RENO-114
- [x] Sub-tareas creadas para Sprint 6: RENO-115 a RENO-120
- [x] Story points estimados y validados
- [x] Sprint 5 y 6 planificados en Jira
- [x] Dependencias configuradas entre tickets
- [ ] Daily stand-ups con actualización de tickets
- [ ] Sprint review y retrospectiva documentadas

### Implementación (GitHub)
- [x] GitHub issues creados: #150, #151, #152
- [x] Milestone v2.1.0 creado
- [x] PRs enlazados con Jira: usar formato "RENO-XXX: título"
- [x] Smart commits habilitados y probados
- [x] Branch strategy: `feature/RENO-XXX-description`
- [ ] Code reviews completadas
- [ ] PRs mergeados a `main`
- [ ] Tags de release creados

### Renovatio MCP
- [ ] Workspace COBOL configurado
- [ ] Ejecutar `cobol.analyze` → [RENO-110](https://jira/RENO-110)
- [ ] Ejecutar `cobol.metrics` → [RENO-111](https://jira/RENO-111)
- [ ] Ejecutar `cobol.migrate_db2` → [RENO-113](https://jira/RENO-113)
- [ ] Ejecutar `cobol.plan` → [RENO-114](https://jira/RENO-114)
- [ ] Ejecutar `cobol.apply` (dry-run) → [RENO-115](https://jira/RENO-115)
- [ ] Ejecutar `cobol.apply` (producción) → [RENO-117](https://jira/RENO-117)
- [ ] Ejecutar `java.format` → [RENO-105](https://jira/RENO-105)
- [ ] Ejecutar `java.test` → [RENO-118](https://jira/RENO-118)

### Validación
- [ ] Compilación exitosa (0 errores)
- [ ] Tests unitarios pasan (100%)
- [ ] Tests integración pasan (100%)
- [ ] Coverage ≥ 80%
- [ ] Validación funcional completada → [RENO-119](https://jira/RENO-119)
- [ ] Performance tests ejecutados
- [ ] Security scan completado

### Documentación
- [ ] README del proyecto generado actualizado
- [ ] Guía de arquitectura → [RENO-120](https://jira/RENO-120)
- [ ] Mapeos COBOL-Java documentados
- [ ] Ejemplos de uso añadidos
- [ ] API docs (OpenAPI/Swagger) generadas
- [ ] Changelog actualizado

### Sincronización Jira-GitHub
- [x] GitHub-Jira app activa y configurada
- [x] Webhooks funcionando
- [x] Estados mapeados: To Do, In Progress, In Review, Done
- [x] Transiciones automáticas validadas
- [ ] Métricas de burndown actualizadas en Jira

## 8. Seguimiento y comunicación

### Stakeholders

| Rol | Nombre | Responsabilidad | Contacto |
|-----|--------|-----------------|----------|
| Product Owner | @po-name | Priorización y criterios de aceptación | PO en Jira |
| Tech Lead | @tech-lead-name | Arquitectura y decisiones técnicas | Reviewer en PRs |
| Arquitecto | @architect-name | Diseño de arquitectura JPA | Reviewer en [RENO-101](https://jira/RENO-101) |
| Especialista COBOL | @cobol-expert | Validación de lógica COBOL | Consultor |
| DBA | @dba-name | Estructura DB2 y optimización | Documentación DDL |
| QA Lead | @qa-lead | Testing y validación | Owner [RENO-118](https://jira/RENO-118) |

### Comunicación

- **Kickoff meeting**: Realizada ✅ (documentada en [RENO-101](https://jira/RENO-101))
- **Daily stand-ups**: 10:00 AM (actualizar tickets Jira)
- **Sprint planning**: Inicio de cada sprint (documentar en Jira)
- **Sprint review**: Final de cada sprint (demo + retrospectiva)
- **Slack channel**: `#renovatio-customer-migration`
- **Jira board**: https://your-company.atlassian.net/secure/RapidBoard.jspa?rapidView=123

### Reportes de progreso

- **Burndown chart**: Actualizado automáticamente en Jira
- **Velocity**: Tracking en Jira (Sprint 5 & 6)
- **Status report**: Semanal (actualizar spec + comentario en [RENO-100](https://jira/RENO-100))

## 9. Apéndice

### A. Ejemplo de mapeo COBOL → Java/JPA

**COBOL (CUSTPROG.cbl):**
```cobol
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CUSTPROG.
       
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  CUSTOMER-RECORD.
           05  CUST-ID        PIC 9(8).
           05  CUST-NAME      PIC X(50).
           05  CUST-EMAIL     PIC X(100).
       
       EXEC SQL
           SELECT CUST_ID, CUST_NAME, CUST_EMAIL
           INTO :CUST-ID, :CUST-NAME, :CUST-EMAIL
           FROM CUSTOMER
           WHERE CUST_ID = :WS-CUST-ID
       END-EXEC.
```

**Java (Customer.java - Entity):**
```java
@Entity
@Table(name = "CUSTOMER")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    
    @Id
    @Column(name = "CUST_ID")
    private Long custId;
    
    @Column(name = "CUST_NAME", length = 50)
    private String custName;
    
    @Column(name = "CUST_EMAIL", length = 100)
    private String custEmail;
}
```

**Java (CustomerRepository.java):**
```java
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    Optional<Customer> findByCustId(Long custId);
    
    @Query("SELECT c FROM Customer c WHERE c.custEmail = :email")
    Optional<Customer> findByEmail(@Param("email") String email);
}
```

**Java (CustomerService.java):**
```java
@Service
@Transactional
public class CustomerService {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    public Customer findById(Long custId) {
        return customerRepository.findByCustId(custId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + custId));
    }
    
    // Migrado desde lógica COBOL del programa CUSTPROG
    public CustomerDTO getCustomerDetails(Long custId) {
        Customer customer = findById(custId);
        return CustomerMapper.toDTO(customer);
    }
}
```

### B. Configuración Spring Boot generada

**application.yml:**
```yaml
spring:
  datasource:
    url: jdbc:db2://localhost:50000/CUSTDB
    username: ${DB2_USER}
    password: ${DB2_PASSWORD}
    driver-class-name: com.ibm.db2.jcc.DB2Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.DB2Dialect
        format_sql: true
    show-sql: false
  
  application:
    name: customer-management
    
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### C. Enlaces útiles

- **Jira Epic**: [RENO-100](https://your-company.atlassian.net/browse/RENO-100)
- **Jira Board**: https://your-company.atlassian.net/secure/RapidBoard.jspa?rapidView=123
- **GitHub Milestone**: [v2.1.0](https://github.com/accentureshark/renovatio/milestone/5)
- **Confluence docs**: https://your-company.atlassian.net/wiki/spaces/RENO/pages/123456
- **Slack channel**: `#renovatio-customer-migration`

### D. Lecciones aprendidas (post-implementation)

_A completar al finalizar el proyecto_

---

**Esta especificación demuestra el uso completo de Jira integration en el modelo spec-driven de @github/spec-kit.**

**Última actualización**: 2025-10-18  
**Estado**: In Progress (Sprint 5, Semana 2)  
**Próxima revisión**: 2025-10-25
