# Deprecation plan

## Compatibility boundary

`org.shark.renovatio.core.service.TargetEmitterRegistry` remains only as a deprecated adapter over `org.shark.renovatio.shared.emission.TargetEmitterRegistry`. No provider depends on the adapter.

## Removal

- Owner: Renovatio maintainers.
- Target: the next major release after all Core consumers import the shared registry.
- Exit check: repository search returns no imports or constructions of the Core adapter outside its compatibility tests.

The `registryRouting` switch and `generateInterfaceStubsLegacy` method are removed in this correction. Constructor boolean parameters are retained temporarily for source compatibility but do not select a second runtime route.
