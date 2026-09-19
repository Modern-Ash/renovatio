---
schema: "agora/work/v1"
id: "ollama-cobol-model-benchmark"
swarm: "llm-cobol-discovery-epic"
title: "Benchmark: OllamaLlmProvider + modelos COBOL locales (qwen2.5-cobol-coder, granite-code) vs Anthropic/Gemini"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"provider-implemented":"OllamaLlmProvider implementado y testeado con fake/mock","two-models-evaluated":"Al menos dos modelos locales evaluados contra el mismo prompt/input real","objective-comparison":"Comparaci\u00f3n documentada con criterios objetivos, no solo impresi\u00f3n subjetiva","recommendation-made":"Recomendaci\u00f3n expl\u00edcita de para qu\u00e9 prompts conviene Ollama vs proveedores remotos"}
satisfied-criteria: []
criterion-statuses: {"provider-implemented":[],"two-models-evaluated":[],"objective-comparison":[],"recommendation-made":[]}
required-artifacts: []
child-work-refs: []
budget-limits: null
parent-work: "llm-cobol-discovery-epic/epic-llm-cobol-discovery"
---

# Benchmark: OllamaLlmProvider + modelos COBOL locales (qwen2.5-cobol-coder, granite-code) vs Anthropic/Gemini

## Description

Parte de epic-llm-cobol-discovery. Depende de que al menos una de las issues que leen texto COBOL crudo (control-break-detection-llm-eval o copy-replacing-support-eval) tenga un prompt implementado para poder benchmarkear contra algo real.

## Objetivo
Agregar un `OllamaLlmProvider` (implementando la interfaz `LlmProvider` existente) y usarlo para comparar, sobre los prompts de este epic que sí leen texto COBOL crudo (no el facts-block determinista de `epic-llm-domain-entities`, que evita depender de fluidez COBOL del modelo por diseño), un modelo COBOL-especializado local y gratuito contra los proveedores ya integrados (Anthropic, Gemini).

## Contexto técnico necesario
- Interfaz a implementar: `renovatio-llm/src/main/java/org/modernash/renovatio/llm/provider/LlmProvider.java` — revisar `AnthropicLlmProvider.java`/`GeminiLLMProvider.java` como plantilla directa de forma/responsabilidades. Ollama expone un endpoint HTTP compatible con OpenAI en `http://localhost:11434/v1` (o su API nativa en `/api/generate`/`/api/chat`) — cualquiera de los dos sirve, revisar cuál de los dos providers existentes es más fácil de adaptar según si ya usan un cliente HTTP genérico o un SDK específico de cada proveedor.
- Modelos candidatos a probar (identificados en Hugging Face/Ollama, no evaluados todavía en este repo):
  - `sammcj/qwen2.5-cobol-coder-7b-instruct` — ya publicado en Ollama nativo (`ollama pull sammcj/qwen2.5-cobol-coder-7b-instruct`), Qwen2.5-7B fine-tuneado en COBOL. Candidato principal por ser el más directo de correr (sin import manual de GGUF).
  - `mradermacher/COBOL-Coder-7B-Instruct-GGUF` (y su variante `-i1-GGUF`) — importable a Ollama vía `ollama pull hf.co/mradermacher/COBOL-Coder-7B-Instruct-GGUF`.
  - `thinkingdbx/cobolx-1.5b-GGUF` — modelo más chico (1.5B), útil para medir el punto de equilibrio tamaño/calidad si el de 7B ya anda bien.
  - `granite-code:20b` (IBM, general-purpose, ya en el catálogo oficial de Ollama) — como control/baseline de "modelo de código fuerte pero NO especializado en COBOL", para poder atribuir cualquier diferencia de calidad a la especialización COBOL y no solo al tamaño/arquitectura del modelo.
  - Ninguno de estos modelos fue probado ni validado en esta sesión — **la calidad real de cada uno para las tareas de discovery de Renovatio es una incógnita total, esto es lo que esta issue debe medir**, no asumir de antemano.
- Precedente de infraestructura de evaluación a reusar: `renovatio-evals` y el eval harness de `domain-entities-eval-harness` (otro epic) — revisar si conviene compartir el mismo harness/métricas o si este benchmark necesita uno propio más simple (comparación de calidad de output entre proveedores, no necesariamente precisión/recall contra un ground-truth de relaciones).
- **Importante**: los prompts de `epic-llm-domain-entities` (`cobol.domain.entities.v1`) NO son un buen caso de prueba para este benchmark porque están diseñados para recibir el facts-block determinista, no COBOL crudo — el beneficio de un modelo COBOL-especializado ahí es marginal por diseño. Los casos de prueba reales de este benchmark deben ser los prompts de `control-break-detection-llm-eval` y/o `copy-replacing-support-eval` (si ya tienen un prompt implementado al momento de trabajar esta issue) u otro prompt de este mismo epic que sí lea texto/párrafos COBOL sin pre-procesar.

## Tareas
1. Implementar `OllamaLlmProvider` siguiendo el patrón de `AnthropicLlmProvider`/`GeminiLLMProvider`, apuntando a una instancia local de Ollama (configurable, sin URL hardcodeada — usar el mismo mecanismo de configuración que ya usan los otros providers).
2. Instalar y probar localmente al menos `sammcj/qwen2.5-cobol-coder-7b-instruct` y `granite-code:20b` como baseline de control.
3. Elegir un prompt de este epic que ya lea texto COBOL crudo (esperar a que `control-break-detection-llm-eval` o `copy-replacing-support-eval` tengan al menos un prompt YAML implementado) y correrlo contra: Ollama+qwen2.5-cobol-coder, Ollama+granite-code:20b, y el proveedor ya en uso (Anthropic o Gemini) — mismo input, comparar outputs.
4. Definir criterios de comparación objetivos (no solo "se ve mejor"): ¿pasa el validador de grounding del prompt correspondiente?, ¿coincide con un ground-truth manual conocido (reusar los mismos casos de CardDemo ya documentados en otras issues de este epic si aplican)?, latencia local vs. remota, costo (cero para Ollama local vs. costo por token de los proveedores remotos).
5. Documentar el resultado con una recomendación concreta: ¿conviene ofrecer Ollama+modelo-COBOL como alternativa gratuita para alguno de los prompts de discovery?, ¿para cuáles sí y para cuáles no (ej. probablemente no para `cobol.domain.entities.v1` dado el punto anterior)?

## Criterios de aceptación
- [ ] `OllamaLlmProvider` implementado, testeado con al menos un fake/mock (no depender de que Ollama esté corriendo en CI).
- [ ] Al menos dos modelos locales (uno COBOL-especializado, uno genérico de control) evaluados contra el mismo prompt/input real.
- [ ] Comparación documentada con criterios objetivos (grounding, coincidencia con ground-truth, latencia, costo) — no solo impresión subjetiva.
- [ ] Recomendación explícita de para qué prompts (si alguno) conviene ofrecer la vía Ollama+COBOL como alternativa a Anthropic/Gemini.

## Evidencia a dejar en el PR
- Tests de `OllamaLlmProvider`.
- Tabla comparativa de resultados (modelo, prompt, pasa-grounding, coincide-ground-truth, latencia, costo).
- Documento de recomendación final.

## Acceptance criteria

- [ ] **provider-implemented:** OllamaLlmProvider implementado y testeado con fake/mock; stages: none
- [ ] **two-models-evaluated:** Al menos dos modelos locales evaluados contra el mismo prompt/input real; stages: none
- [ ] **objective-comparison:** Comparación documentada con criterios objetivos, no solo impresión subjetiva; stages: none
- [ ] **recommendation-made:** Recomendación explícita de para qué prompts conviene Ollama vs proveedores remotos; stages: none

## Required artifacts

- none
