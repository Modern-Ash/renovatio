# Prompt: Generar Feature Files Gherkin desde Especificación

**Propósito**: Convertir historias de usuario de `spec.md` en feature files ejecutables de Cucumber/Gherkin.

---

## Contexto

Este proyecto usa **Spec-Driven Development (SDD)** con testing BDD. Después de crear `spec.md`, debes generar feature files en formato Gherkin que sirvan como:
- Tests ejecutables (behave para backend, @cucumber/cucumber para frontend)
- Documentación viva que stakeholders no técnicos pueden leer
- Validación de criterios de aceptación

---

## Instrucciones para IA

### Input Esperado

Lee el archivo `/specs/NNN-feature/spec.md` y extrae:
1. Cada **Historia de Usuario** (User Story)
2. Sus **Escenarios de Aceptación** (Given-When-Then)
3. Los **Requisitos Funcionales** (FR-XXX) asociados
4. El **Epic/Story de Jira** vinculado

### Output Esperado

Genera feature files en las siguientes ubicaciones:

```
apps/api/features/           # Si la feature toca el backend
apps/dashboard/features/     # Si la feature toca el frontend
apps/services/features/      # Si es orquestación/workers
```

### Plantilla de Feature File

```gherkin
@JIRA:TRAV-XXX
@P1
Feature: [Título de la User Story desde spec.md]
  Como [rol del usuario]
  Necesito [capacidad]
  Para [beneficio/objetivo]

  Background:
    Given el sistema está inicializado
    And la base de datos de test está limpia

  @smoke @FR-001
  Scenario: [Primer escenario de aceptación]
    Given [condición inicial del spec.md]
    When [acción del spec.md]
    Then [resultado esperado del spec.md]
    And [validación adicional]

  @regression @FR-002
  Scenario: [Segundo escenario de aceptación]
    Given [otra condición]
    When [otra acción]
    Then [otro resultado]

  @error-handling
  Scenario Outline: [Escenario parametrizado si aplica]
    Given un <entity> con <field> "<value>"
    When se ejecuta <action>
    Then el resultado es "<expected>"

    Examples:
      | entity | field  | value  | action  | expected |
      | user   | email  | test@  | create  | invalid  |
      | user   | email  | a@b.c  | create  | success  |
```

---

## Reglas de Conversión

### 1. Tags Obligatorios

Cada feature DEBE tener:
- `@JIRA:TRAV-XXX` - Número de story de Jira desde spec.md
- `@P1`, `@P2`, o `@P3` - Prioridad desde spec.md
- `@smoke` - Escenarios críticos (happy path)
- `@regression` - Escenarios de casos límite
- `@FR-XXX` - Tags de requisitos funcionales

### 2. Background

Si múltiples escenarios comparten setup común:
```gherkin
Background:
  Given el sistema está inicializado
  And existe un usuario autenticado con email "test@example.com"
  And la base de datos tiene datos de fixture
```

### 3. Scenario vs Scenario Outline

Usa `Scenario Outline` cuando:
- Necesitas testear múltiples variaciones del mismo flujo
- Los datos varían pero la lógica es idéntica

Ejemplo:
```gherkin
Scenario Outline: Clasificar emails de proveedores
  Given un email con asunto "<subject>"
  When el clasificador de IA lo analiza
  Then el estado debe ser "<status>"

  Examples:
    | subject                  | status    |
    | Confirmación Booking 123 | confirmed |
    | Lamentamos informar      | rejected  |
    | Necesitamos más info     | doubt     |
```

### 4. Mapeo de Escenarios de Aceptación

Desde `spec.md`:
```markdown
**Dado** que llega un correo, **Cuando** el sistema lo procesa, **Entonces** extrae metadatos
```

A `feature` file:
```gherkin
Scenario: Procesar correo entrante exitosamente
  Given un correo nuevo llega a la bandeja monitoreada
  When el sistema ejecuta el procesamiento
  Then el asunto es extraído correctamente
  And el cuerpo es parseado sin errores
  And los adjuntos son almacenados en GCS
```

### 5. DocStrings para Datos Complejos

Cuando un step necesita JSON/XML/texto multi-línea:
```gherkin
Given el siguiente correo en formato MIME:
  """
  From: hotel@example.com
  Subject: Confirmación Reserva #12345
  
  Estimado cliente,
  Confirmamos su reserva...
  """
```

### 6. DataTables para Listas

Cuando un step necesita múltiples registros:
```gherkin
Given las siguientes suscripciones activas:
  | subscriptionId | clientId | imapHost       |
  | sub-001        | client-1 | imap.gmail.com |
  | sub-002        | client-2 | outlook.com    |
```

---

## Estrategia de Ubicación

### Backend (apps/api/features/)

Crea feature file aquí si la user story:
- Define un nuevo endpoint REST
- Modifica lógica de negocio del backend
- Valida esquemas Pydantic
- Requiere consultas a Firestore

**Naming**: `{entity}_{action}.feature`
- `subscriptions_create.feature`
- `emails_parse.feature`
- `auth_login.feature`

### Frontend (apps/dashboard/features/)

Crea feature file aquí si la user story:
- Define comportamiento de un componente React
- Valida interacciones de usuario (clicks, forms)
- Testea navegación entre páginas
- Verifica renderizado de datos

**Naming**: `{component}_{interaction}.feature`
- `service_card_display.feature`
- `profile_form_validation.feature`
- `subscription_list_filter.feature`

### Services (apps/services/features/)

Crea feature file aquí si la user story:
- Define un job/worker de background
- Orquesta múltiples servicios (API + scrapper)
- Ejecuta pipelines de datos
- Usa LLM o procesamiento por lotes

**Naming**: `{workflow}_{operation}.feature`
- `email_monitoring_polling.feature`
- `ai_classification_batch.feature`

---

## Ejemplo Completo

### Input: spec.md

```markdown
### Historia de Usuario 1 - Recepción de Correos (Prioridad: P1)

**Historia Jira**: TRAV-2
**Jira Link**: https://faguero.atlassian.net/browse/TRAV-2

Como sistema TravelCBooster, necesito detectar correos entrantes de proveedores.

**Escenarios de Aceptación**:
1. **Dado** que llega un correo, **Cuando** se procesa, **Entonces** extrae metadatos
2. **Dado** que un correo tiene adjuntos, **Cuando** se procesa, **Entonces** los guarda en GCS
```

### Output: apps/services/features/email_reception.feature

```gherkin
@JIRA:TRAV-2
@P1
Feature: Recepción de Correos de Proveedores
  Como sistema TravelCBooster
  Necesito detectar correos entrantes de proveedores
  Para poder procesarlos automáticamente

  Background:
    Given el sistema de monitoreo está activo
    And existe una suscripción con configuración IMAP válida

  @smoke @FR-001
  Scenario: Procesar correo con metadatos básicos
    Given un correo nuevo llega a la bandeja monitoreada
    And el correo tiene asunto "Confirmación Booking 12345"
    And el correo tiene remitente "hotel@example.com"
    When el sistema ejecuta el ciclo de polling
    Then el correo es detectado correctamente
    And el asunto "Confirmación Booking 12345" es extraído
    And el remitente "hotel@example.com" es registrado
    And el messageId es almacenado en Firestore

  @smoke @FR-002
  Scenario: Procesar correo con adjuntos PDF
    Given un correo nuevo con 2 archivos adjuntos
    And el primer adjunto es "voucher.pdf" de 150KB
    And el segundo adjunto es "invoice.pdf" de 200KB
    When el sistema procesa los adjuntos
    Then ambos archivos son subidos a Google Cloud Storage
    And las URLs de GCS son registradas en Firestore
    And los metadatos incluyen nombre y tamaño de archivos

  @error-handling
  Scenario: Manejar correo malformado
    Given un correo con headers MIME corruptos
    When el sistema intenta parsear el correo
    Then se registra un error de tipo "MIME_PARSE_ERROR"
    And el correo es movido a carpeta "errores"
    And se emite una alerta de monitoreo
```

---

## Checklist de Validación

Antes de finalizar, verifica:
- [ ] Cada User Story tiene al menos 1 feature file
- [ ] Cada escenario de aceptación del spec.md está cubierto
- [ ] Tags de Jira coinciden con spec.md
- [ ] Tags de prioridad (P1/P2/P3) están presentes
- [ ] Tags de FR-XXX vinculan requisitos funcionales
- [ ] El lenguaje es claro para stakeholders no técnicos
- [ ] Los steps son atómicos y reutilizables
- [ ] Background evita duplicación entre escenarios

---

## Comandos de Ejecución

Después de generar los feature files, documenta cómo ejecutarlos:

```bash
# Backend (Python/behave)
cd apps/api && behave
cd apps/api && behave --tags=@JIRA:TRAV-2
cd apps/api && behave --tags=@smoke

# Frontend (TypeScript/Cucumber)
cd apps/dashboard && npm run test:bdd
cd apps/dashboard && npm run test:bdd -- --tags "@P1"

# Services (Python/behave)
cd apps/services && behave --tags=@FR-001
```

---

## Siguientes Pasos

Después de generar feature files:
1. **Ejecutar sin step definitions** - DEBE FALLAR con "steps undefined"
2. **Generar stubs de step definitions** - `behave --dry-run --no-capture --no-capture-stderr`
3. **Implementar step definitions** - En `apps/*/steps/*_steps.py` o `*_steps.ts`
4. **Ejecutar tests** - DEBE PASAR cuando implementación esté completa

---

## Integración con SDD

Este paso ocurre **DESPUÉS** de crear `spec.md` y **ANTES** de implementar código:

```
1. spec.md (requisitos en lenguaje natural)
2. → ESTE PASO: Generar feature files Gherkin
3. plan.md (decisiones de arquitectura)
4. data-model.md (esquemas de datos)
5. tasks.md (desglose de tareas)
6. → Implementar step definitions (tests que fallan)
7. → Implementar código (hacer pasar tests)
```

---

**Versión**: 1.0.0  
**Última Actualización**: 2025-11-20  
**Autor**: TravelCBooster Team
