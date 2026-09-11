# Threat Model: LLM Provider Gobernado (STRIDE)

## Contexto

Este threat model cubre la integración del LLM como proveedor gobernado de propuestas en el pipeline de Renovatio. El LLM **nunca** tiene autoridad de escritura, ejecución o decisión final — solo propone.

---

## Assets

| Asset | Clasificación | Descripción |
|-------|---------------|-------------|
| Source COBOL | CONFIDENTIAL | Código legado del cliente |
| ProposalRequest | INTERNAL | Input al LLM (hash + contexto mínimo) |
| TypedProposal | INTERNAL | Output del LLM (validado) |
| Governance Log | RESTRICTED | Auditoría completa con hashes |
| DecisionSet | RESTRICTED | Decisiones humanas sobre propuestas |
| LLM Config | SECRET | API keys, endpoints, budgets |
| Model Weights | N/A | No almacenados localmente |

---

## STRIDE Analysis

### S - Spoofing (Suplantación)

| Amenaza | Vector | Impacto | Likelihood | Mitigación |
|---------|--------|---------|------------|------------|
| LLM se hace pasar por proveedor legítimo | Compromiso de endpoint Vertex/Gemini | Propuestas maliciosas aceptadas | Baja | mTLS, API key rotation, endpoint pinning |
| Attacker inyecta ProposalRequest falsificado | Intercepción red / API comprometida | Procesamiento de input malicioso | Media | Input validation, sourceHash verification, schema validation |
| Fake provider suplantado en config | Config tampering | Bypass de gobernanza | Baja | Config signed, immutable at runtime |

**Residual Risk**: LOW — mTLS + schema validation + hash verification

---

### T - Tampering (Manipulación)

| Amenaza | Vector | Impacto | Likelihood | Mitigación |
|---------|--------|---------|------------|------------|
| Modificación de TypedProposal en tránsito | MITM / memory corruption | Propuesta alterada aceptada | Baja | outputHash verification, immutable evidence log |
| Alteración de governance log | Log injection / disk tampering | Pérdida de auditoría | Media | Append-only log, hash chaining, WORM storage |
| Modificación de DecisionSet | DB tampering | Decisiones falsificadas | Baja | DecisionSet immutable, rationaleHash verification |
| Prompt injection en request | Source COBOL malicioso | LLM genera output controlado | Alta | PromptSanitizer, context trimming, no exec LLM output |

**Residual Risk**: MEDIUM — Prompt injection es el vector principal; mitigado por sanitization + no execution

---

### R - Repudiation (Repudio)

| Amenaza | Vector | Impacto | Likelihood | Mitigación |
|---------|--------|---------|------------|------------|
| Actor niega haber aprobado propuesta | Falta de non-repudiation | Disputas de gobernanza | Media | Actor tracking, timestamp, rationaleHash (no content) |
| LLM provider niega haber generado output | Sin attestation | Imposible auditar | Media | outputHash + promptHash + model version en metadata |
| Sistema niega haber procesado request | Falta de audit trail | Compliance failure | Baja | Immutable audit log con hash chaining |

**Residual Risk**: LOW — Hash chains + actor tracking + timestamps

---

### I - Information Disclosure (Divulgación)

| Amenaza | Vector | Impacto | Likelihood | Mitigación |
|---------|--------|---------|------------|------------|
| Source COBOL enviado a LLM remoto | Config incorrecta / no offline default | IP leakage a proveedor externo | Media | **offlineDefault=true**, data minimization, context trimming |
| API keys en logs / governance log | Logging accidental | Credential leakage | Media | **No secret persistence**, redaction util, structured logging |
| PII en ProposalRequest context | Datos cliente en contexto | Privacy violation | Media | Redaction: PII, keys, snippets >50 chars |
| Model weights / prompts en repo | Commit accidental | IP leakage | Baja | .gitignore, secret scanning CI |

**Residual Risk**: LOW — offlineDefault + redaction + data minimization + secret scanning

---

### D - Denial of Service (Denegación)

| Amenaza | Vector | Impacto | Likelihood | Mitigación |
|---------|--------|---------|------------|------------|
| LLM remoto inalcanzable / lento | Network / provider outage | Pipeline bloqueado | Alta | **fake provider default**, circuit breaker, timeout |
| Budget agotado (tokens/cost) | Uso intensivo / attack | LLM inutilizable | Media | Budget enforcement, rate limiting, circuit breaker |
| Prompt DoS (contexto masivo) | Source grande / attack | Timeout / OOM | Media | Context trimming, maxTokens budget, input size limit |
| Rate limit exceeded | Burst requests | Throttling / errores | Media | Exponential backoff, queue, fallback a fake |

**Residual Risk**: LOW — Multiple layers: fake default + circuit breaker + budgets + timeouts

---

### E - Elevation of Privilege (Escalada)

| Amenaza | Vector | Impacto | Likelihood | Mitigación |
|---------|--------|---------|------------|------------|
| LLM output ejecutado como código | Falta de boundary | RCE / transformaciones no autorizadas | Crítica | **LLM nunca escribe/ejecuta**, solo propone; DecisionSet required |
| LLM accede a filesystem / DB | SDK permissions | Data exfiltration / corruption | Crítica | **Sin SDKs LLM en core**, fake provider sin network |
| Config tampering para elevar LLM | Privilege escalation | Bypass gobernanza | Media | Config immutable at runtime, signed |
| DecisionSet bypass | Auto-accept logic | Decisiones sin humano | Alta | **Human-in-the-loop obligatorio**, low confidence → reject |

**Residual Risk**: LOW — Arquitectura por diseño: LLM = propuesta, Humano = decisión, Core = ejecución

---

## Resumen de Riesgos

| Categoría | Riesgo Inicial | Residual | Estado |
|-----------|----------------|----------|--------|
| Spoofing | Medium | Low | ✅ Mitigado |
| Tampering | High | Medium | ⚠️ Prompt injection residual |
| Repudiation | Medium | Low | ✅ Mitigado |
| Info Disclosure | High | Low | ✅ Mitigado |
| DoS | High | Low | ✅ Mitigado |
| Elevation | Critical | Low | ✅ Mitigado (by design) |

---

## Controles Críticos (Must Have)

1. **offlineDefault=true** — Fake provider por defecto, sin red
2. **No execution authority** — LLM solo propone, DecisionSet decide
3. **No SDKs in core** — Dependencia unidireccional: core → llm-runtime (interface)
4. **Schema validation** — Toda propuesta validada contra JSON Schema v1
5. **Human-in-the-loop** — Low confidence / high risk → spec-owner decide
6. **Immutable audit** — Hash chaining, no secrets, 90d retention
5. **Data minimization** — Solo hash + contexto mínimo al LLM
6. **Circuit breaker + budgets** — Failure modes no bloquean core

---

## Verificación

| Control | Test | Frecuencia |
|---------|------|------------|
| offlineDefault | Integration test: no network calls en fake mode | CI/PR |
| No execution | ArchUnit: no LLM SDK imports en renovatio-cobol-ir, renovatio-provider-cobol, renovatio-jcl | CI/PR |
| Schema validation | Contract test: invalid schema → reject | CI/PR |
| Human-in-the-loop | Integration test: low confidence → DecisionSet | CI/PR |
| Audit log | Integration test: hash chain verified | CI/PR |
| Data minimization | Unit test: context >50 chars trimmed | CI/PR |
| Circuit breaker | Chaos test: provider down → fake fallback | CI/PR |
| Budget enforcement | Integration test: budget exceeded → cb open | CI/PR |

---

## Referencias

- OWASP Top 10 for LLM Applications (2023)
- NIST AI RMF (AI Risk Management Framework)
- STRIDE Threat Modeling (Microsoft)
- Renovatio Constitution: `.agora/constitution.md`