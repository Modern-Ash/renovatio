# Correction: committed-cache hit verification classification

The earlier report `llm-committed-cache-hit-20260830.md` correctly observed empty stdout and stderr
from `tool-20260830t19371788129470z`, but described the outer Agora invocation imprecisely as though
no tool-run existed. Agora necessarily recorded the operator's dispatch of the shared executable.

The empty result is evidence that the runtime returned on the committed hit before constructing the
provider and before calling `AttributionGateway.begin` or `complete`. The outer dispatch is not a
successfully finalized cache-miss attribution and does not satisfy the `agora-attribution`
criterion. The operation result kind is now `llm-enrichment-dispatch`; only a reconciled nonempty
miss result may count as attribution. The original immutable report and evidence remain historical,
and this correction supersedes only their classification language.
