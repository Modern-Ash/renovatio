# Verification report

Revalidated after PR #245 review on 2026-09-09:

`./mvnw -q -pl renovatio-architecture,renovatio-provider-cobol,renovatio-api -am test -Djacoco.skip=true`

Result: BUILD SUCCESS across the relevant reactor modules, with zero failures or errors. Added
regressions prove insertion-order-independent decision hashes and projections, canonical component
references from targets/manifests, manifest hash exposure in preview, stale-manifest rejection during
generation, and domain isolation from Spring, Java filesystem APIs, LLM and target packages.
