---
schema: "agora/work/v1"
id: "xmainframe-summarization-benchmark"
swarm: "llm-cobol-discovery-epic"
title: "Benchmark XMAiNframe (Fsoft-AIC) para resumen/explicaci\u00f3n de negocio de COBOL"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"running-locally":"XMAiNframe corriendo localmente, m\u00e9todo de puesta en marcha documentado","qualitative-comparison":"Comparaci\u00f3n cualitativa de res\u00famenes vs proveedor remoto sobre casos reales de CardDemo","scale-degradation-checked":"Evaluaci\u00f3n expl\u00edcita de si la degradaci\u00f3n multi-programa observada con qwen2.5-cobol-coder tambi\u00e9n afecta a XMAiNframe","recommendation-made":"Recomendaci\u00f3n final de en qu\u00e9 tareas conviene XMAiNframe"}
satisfied-criteria: []
criterion-statuses: {"running-locally":[],"qualitative-comparison":[],"scale-degradation-checked":[],"recommendation-made":[]}
required-artifacts: []
child-work-refs: []
budget-limits: null
parent-work: "llm-cobol-discovery-epic/epic-llm-cobol-discovery"
---

# Benchmark XMAiNframe (Fsoft-AIC) para resumen/explicación de negocio de COBOL

## Description

Parte de epic-llm-cobol-discovery. Complementa (no reemplaza) a ollama-cobol-model-benchmark: ese ticket evalúa modelos ya nativos de Ollama para extracción/generación; este evalúa específicamente XMAiNframe para la tarea distinta de "explicar en términos de negocio", que requiere otra vía de distribución.

## Objetivo
Evaluar `Fsoft-AIC/XMAiNframe-instruct-10.5b` (y/o la variante `-7b`) — un LLM entrenado específicamente sobre mainframe/COBOL (sobre base DeepSeek-Coder) — para las tareas de este epic que son fundamentalmente "explicar/resumir qué hace este código en términos de negocio", donde el propio benchmark publicado del modelo (MainframeBench) lo posiciona muy por encima de modelos generalistas.

## Contexto técnico necesario
- **Confirmado por búsqueda externa** (no evaluado dentro de este repo todavía): XMAiNframe está construido sobre DeepSeek-Coder (7B y 10.5B), y en la tarea de COBOL code summarization de `MainframeBench` obtiene BLEU-4 = 62.58, contra 11.37 de GPT-3.5 y 7.42 de GPT-4 (~6x y ~9x respectivamente). En knowledge Q&A de mainframe, duplica el BLEU de Mixtral-Instruct-8x7B, y en preguntas de opción múltiple le saca 30% de accuracy a DeepSeek-Coder base.
  - Modelo: `Fsoft-AIC/XMAiNframe-instruct-10.5b` (también existe `-7b`) en Hugging Face.
  - Pipeline/código de referencia: `FSoft-AI4Code/XMainframe` en GitHub (incluye cómo correrlo localmente).
  - Benchmark: `Fsoft-AIC/MainframeBench` en Hugging Face (subsets de Q&A, multiple-choice, y summarization de COBOL) — útil también como fixture de evaluación propio, no solo como referencia de por qué el modelo es bueno.
- **Limitación reconocida en la literatura del propio modelo** (importante para no sobre-prometer): todos los benchmarks actuales de este tipo (MainframeBench incluido, y `CobolCodeBench`) evalúan **programas sueltos**, no sistemas completos con copybooks compartidos, JCL, y dependencias cruzadas entre programas — que es exactamente el caso real de CardDemo (44 programas, 62 copybooks) que se usa como banco de pruebas en este repo. Ningún modelo publicado está evaluado para ese escenario de sistema completo — la prueba grande ya corrida en esta sesión con `qwen2.5-cobol-coder-7b` confirmó justamente eso: a escala de varios programas concatenados, la calidad se degrada (entidades perdidas, fusiones incompletas, evidencia de relación más débil).
- **Restricción práctica de distribución**: a diferencia de `sammcj/qwen2.5-cobol-coder-7b-instruct` (ya probado en esta sesión, nativo de Ollama), XMAiNframe **no tiene GGUF publicado ni está en el catálogo de Ollama** — solo pesos `safetensors` de HuggingFace. Correrlo requiere: (a) convertir a GGUF con el script de conversión de `llama.cpp` y cuantizar, para poder usarlo vía Ollama igual que el resto de los providers de este epic (consistente con `OllamaLlmProvider` de `ollama-cobol-model-benchmark`), o (b) servirlo directo vía `transformers`/vLLM detrás de un provider HTTP propio — evaluar cuál conviene según el esfuerzo real de conversión (probar primero la variante 7B por ser más liviana).
- **Dónde encaja en las tareas ya scopeadas de este epic**: la tarea central de XMAiNframe (explicar qué hace un fragmento de código en lenguaje de negocio) es exactamente lo que necesitan `residual-discovery-llm-routing` (clasificar/explicar un `UnclassifiedDataAccess`) y `control-break-detection-llm-eval` (entender la intención de un párrafo de control-break) — y también mejora potencialmente la calidad de nombres de entidad propuestos por `cobol.domain.entities.v1` del otro epic (hoy ese prompt no usa un modelo especializado en explicar negocio, solo en proponer estructura). No reemplaza al facts-block determinista para extracción de relaciones (esa conclusión ya se validó en esta sesión) — es candidato específicamente para la sub-tarea de "explicar/nombrar", no de "extraer estructura".

## Tareas
1. Conseguir un GGUF corriendo localmente (convertir la variante 7B primero, más liviana) o, si la conversión resulta muy costosa, montar un provider HTTP contra `transformers`/vLLM sirviendo el modelo en `safetensors` directo.
2. Correr XMAiNframe contra al menos 2-3 programas reales de CardDemo (empezar con los ya usados en esta sesión: `CBACT01C.cbl`, `CBTRN02C.cbl`) pidiendo un resumen en términos de negocio de qué hace cada uno — comparar cualitativamente contra lo que ya generan los prompts existentes `cobol.domain.naming.v1`/`cobol.unsupported.explain.v1` con Anthropic/Gemini.
3. Probar específicamente la tarea de "explicar un `UnclassifiedDataAccess`" (para `residual-discovery-llm-routing`) y "explicar la intención de un párrafo con forma de control-break" (para `control-break-detection-llm-eval`) con XMAiNframe, y comparar contra la misma prueba con el proveedor remoto ya en uso.
4. Documentar honestamente si la degradación observada con `qwen2.5-cobol-coder-7b` a escala de varios programas concatenados también aplica a XMAiNframe, o si su entrenamiento específico en summarization lo hace más robusto a ese escenario — este es el dato más valioso que puede aportar esta issue, dado que es el gap explícitamente reconocido en la literatura.
5. Recomendación final: ¿para cuáles de las tareas de discovery de este epic conviene XMAiNframe sobre el proveedor remoto ya en uso, considerando calidad, costo-cero, y el esfuerzo de mantener la conversión/serving local?

## Criterios de aceptación
- [ ] XMAiNframe corriendo localmente (GGUF vía Ollama o servido vía transformers/vLLM), documentando el método elegido y su costo de puesta en marcha.
- [ ] Comparación cualitativa de resúmenes/explicaciones de negocio sobre al menos 2 programas reales de CardDemo, contra el proveedor remoto ya en uso.
- [ ] Evaluación explícita de si la degradación a escala multi-programa (observada con qwen2.5-cobol-coder-7b en esta sesión) también afecta a XMAiNframe.
- [ ] Recomendación final documentada: en qué tareas de discovery conviene, en cuáles no.

## Evidencia a dejar en el PR
- Método de puesta en marcha local (conversión GGUF o serving) documentado y reproducible.
- Comparación de salidas (XMAiNframe vs. proveedor remoto) sobre casos reales.
- Conclusión sobre el comportamiento a escala multi-programa.

## Acceptance criteria

- [ ] **running-locally:** XMAiNframe corriendo localmente, método de puesta en marcha documentado; stages: none
- [ ] **qualitative-comparison:** Comparación cualitativa de resúmenes vs proveedor remoto sobre casos reales de CardDemo; stages: none
- [ ] **scale-degradation-checked:** Evaluación explícita de si la degradación multi-programa observada con qwen2.5-cobol-coder también afecta a XMAiNframe; stages: none
- [ ] **recommendation-made:** Recomendación final de en qué tareas conviene XMAiNframe; stages: none

## Required artifacts

- none
