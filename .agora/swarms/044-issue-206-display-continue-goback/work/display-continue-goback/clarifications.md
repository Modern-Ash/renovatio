---
schema: "agora/clarifications/v1"
swarm: "issue-206-display-continue-goback"
work: "display-continue-goback"
created-at: "2026-09-08T10:22:21.471993Z"
last-run-input-sha256: "4eb96bb9a2ff203a6a040833fa5bb865148e5cc63ab227ed1d98ed72a227cc8e"
last-run-question-count: 4
last-run-unanswered-count: 0
last-run-by: "project:owner"
last-run-at: "2026-09-08T10:23:15.851290Z"
---

# Clarifications for display-continue-goback

| Question | Answer | Actor | Timestamp | Input SHA-256 |
| --- | --- | --- | --- | --- |
| ¿Cuál es el formato canónico exacto para sentencias no traducidas: `// <texto COBOL> — not translated yet`, `// <verbo>: not translated yet` u otro? |  | project:owner | 2026-09-08T10:22:21.471993Z | 243d952d8eb101767d6c4777e653efe71f6e64db964751e1fd437154c4320c64 |
| ¿`STOP` sin operandos también debe traducirse como `return <outputVar>;`, aunque los requisitos y criterios de aceptación sólo mencionan `STOP RUN`? |  | project:owner | 2026-09-08T10:22:21.471993Z | 243d952d8eb101767d6c4777e653efe71f6e64db964751e1fd437154c4320c64 |
| Si `GOBACK` o `STOP RUN` aparece antes de otras sentencias en el mismo bloque, ¿el generador debe omitir las sentencias posteriores para evitar errores de Java por código inalcanzable? |  | project:owner | 2026-09-08T10:22:21.471993Z | 243d952d8eb101767d6c4777e653efe71f6e64db964751e1fd437154c4320c64 |
| Para `DISPLAY ... UPON <dispositivo>`, ¿deben renderizarse únicamente los operandos anteriores a `UPON`, excluyendo completamente `UPON` y el dispositivo? | Sí; R1 establece que la cláusula `UPON <dispositivo>` se ignora. | project:owner | 2026-09-08T10:22:21.471993Z | 243d952d8eb101767d6c4777e653efe71f6e64db964751e1fd437154c4320c64 |
| ¿El gate Jacoco de R7 exige 100 % de líneas en los tres módulos o únicamente en `renovatio-provider-cobol`, como indica el criterio `regression-green`? |  | project:owner | 2026-09-08T10:22:21.471993Z | 243d952d8eb101767d6c4777e653efe71f6e64db964751e1fd437154c4320c64 |
| ¿Qué formato de comentario es normativo para una sentencia no traducida: el indicado en la descripción/objetivo o el formato exacto de R4? | Debe prevalecer R4: `// COBOL not translated: <texto COBOL recortado a 120 chars, sin el punto final>`. | project:owner | 2026-09-08T10:23:15.851290Z | 4eb96bb9a2ff203a6a040833fa5bb865148e5cc63ab227ed1d98ed72a227cc8e |
| ¿El criterio `goback-return` debe cubrir también `STOP` sin operandos, aunque su resumen solo mencione GOBACK y STOP RUN? | Sí; R3 define explícitamente que `STOP` a secas se trata como `STOP_RUN`, mientras que `STOP <literal>` queda como `UNTRANSLATED`. | project:owner | 2026-09-08T10:23:15.851290Z | 4eb96bb9a2ff203a6a040833fa5bb865148e5cc63ab227ed1d98ed72a227cc8e |
| ¿Las dos entradas idénticas de `specifications`, con la misma URI, representan un único artefacto `spec` registrado o existe un problema de registro duplicado que debe resolverse antes de pasar a `clarified`? | Representan un único artefacto lógico, ya que tienen la misma URI y contenido. | project:owner | 2026-09-08T10:23:15.851290Z | 4eb96bb9a2ff203a6a040833fa5bb865148e5cc63ab227ed1d98ed72a227cc8e |
| ¿Qué actor asignado debe ejecutar la transición `spec-clarified` y dejar constancia de que todos los criterios están satisfechos? | El `spec-owner`, asignado a `project:owner`; el Developer no puede ejecutar esta transición por su responsabilidad de implementación. | project:owner | 2026-09-08T10:23:15.851290Z | 4eb96bb9a2ff203a6a040833fa5bb865148e5cc63ab227ed1d98ed72a227cc8e |
