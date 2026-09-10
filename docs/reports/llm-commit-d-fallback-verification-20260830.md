# Commit-D and fallback verification

The promotion verifier now discovers the manifest-introducing Commit D from Git history, requires
that commit to change only the manifest, verifies C→D→HEAD ancestry, and compares the manifest at D
with runtime authority. Empty index/manifest authority bypasses D only for a fresh cache miss.

`CatalogFallbackFactory` now emits the strict catalog-declared diagnostic code while the envelope
retains the triggering provider/validation failure category independently.

Java 17 focused dependency reactor: 135 tests passed, zero failures and zero errors. The run includes
the production verifier against actual A/B/C/D repository history and CLI behavior in an empty
fixture repository.
