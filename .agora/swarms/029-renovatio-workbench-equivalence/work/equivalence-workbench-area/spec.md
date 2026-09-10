# Theia 8 · Equivalence area

The Workbench SHALL expose an explicitly read-only Equivalence area for the
selected project. It summarizes the existing evidence and generated-target
inventory available inside that project's configured workspace.

The area SHALL NOT execute a comparison, derive an equivalence verdict,
approve or reject a result, apply a change, or write project state. A missing
or unreadable source is represented as an explicit safe state; it is not a
passing verdict. Dashboard and wizard behavior remain outside this adapter.
