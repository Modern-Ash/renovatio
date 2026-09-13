# Issue 230 Capability Contract

The canonical Java contract lives in:

- `renovatio-application/src/main/java/org/shark/renovatio/application/capability/SurfaceCapabilityRegistry.java`

Required top-level fields:

- `id`
- `version`
- `owner`
- `generatedAt`
- `surfaces`
- `capabilities`

Each capability entry includes:

- `id`
- `summary`
- `operation`
- `sourceLanguage`
- `targetLanguage`
- `maturity`
- `requiredInputs`
- `surfaces`

Surface values are `supported`, `experimental`, or `planned`.
