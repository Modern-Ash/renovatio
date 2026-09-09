# Contrato de aplicación — AC-05

La única autoridad de orquestación es `RenovatioApplication`. Sus nueve operaciones públicas son
independientes de HTTP, CLI, MCP y Spring: `createProject`, `analyzeProject`, `reviewDomain`,
`resolveDecisions`, `plan`, `preview`, `validate`, `apply` y `exportEvidence`.

`plan` y `preview` son queries puras respecto del workspace y de los repositorios finales. Preview
produce un `ArtifactManifest` canónico ligado al `SourceSnapshot`. Validate y apply consumen esas
mismas identidades; nunca recalculan silenciosamente una entrada obsoleta.

Todo comando mutable porta una idempotency key. El repositorio identifica la ejecución por proyecto,
caso de uso y key, y compara el digest canónico del comando. Replay idéntico devuelve el resultado
terminal previo; una key reutilizada con otro digest produce `IdempotencyConflict`.

Apply prepara un `ChangeSet` con preimage, postimage y manifest. La publicación es atómica desde la
perspectiva application: si write, validation o Git local falla, restaura preimage y registra
`REVERTED`. Red y push remoto no forman parte de la transacción.

Los transportes sólo traducen autenticación, forma de entrada y resultados. Parsers, proyectores,
emitters y refinadores existentes se conectan mediante ports; no se duplican dentro de application.
