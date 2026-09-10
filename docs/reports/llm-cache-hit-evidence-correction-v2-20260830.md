# Correction v2: preserve immutable Tool Pack history

Changing the installed `llm-enrichment/enrich` result kind would make its immutable historical
tool-runs fail Tool Pack validation. The operation therefore retains
`llm-enrichment-attribution` for compatibility. Classification instead uses the durable result:
an empty result from a committed hit proves that runtime miss attribution was never initialized or
finalized and cannot satisfy `agora-attribution`; only a nonempty result that passes reconciliation
counts. This report supersedes the proposed result-kind change in the first correction while
preserving both immutable records.
