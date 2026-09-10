# Implementation plan

1. Extend `/api/projects/{projectId}/workbench/ai` from a flat suggestion summary into a governed AI DTO.
2. Reuse existing workbench services as context sources: source explorer, domain model, architecture canvas, shadow impact, and decision layer.
3. Keep command execution declarative: the endpoint exposes slash commands and tool surfaces, but does not mutate files.
4. Render a dedicated governed AI panel in the Theia shell with agents, prompts, commands, context, suggestions, audit, and limits.
5. Add backend and frontend contract coverage for the new surface.
