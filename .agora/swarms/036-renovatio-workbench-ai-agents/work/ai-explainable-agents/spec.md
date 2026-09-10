# Spec

GitHub issue #182 asks for Renovatio AI agents inside the Theia workbench with explainable behavior and guarded execution.

The implementation must expose:

- a stable catalog of specialized assistants: discovery, domain architect, architecture, naming, equivalence, and review;
- versioned prompt metadata for every assistant;
- slash-command entry points mapped to read-only backend tool surfaces;
- a reproducible context snapshot with canonical hashes for source, domain model, architecture, and shadow impact inputs;
- proposal-only recommendations that require explicit human review before any mutation;
- audit metadata for suggestions, including prompt id/version, context hash, response hash, tool calls, status, and approval state.

The UI must make those boundaries visible instead of implying that AI can directly edit source files.
