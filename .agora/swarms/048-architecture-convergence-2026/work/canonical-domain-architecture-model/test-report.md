# Verification report

Executed 2026-09-09:

`./mvnw -pl renovatio-architecture,renovatio-provider-cobol -am test -Djacoco.skip=true`

Result: BUILD SUCCESS across 14 reactor modules. Relevant suites include 18 architecture tests,
24 Java provider tests and 117 COBOL provider tests, with zero failures or errors. The registry-routing
contracts exercise preview and apply over the same preparation path; canonical invariants cover stable
identity/order, provenance, hash changes, schema diagnostics and legacy-view equivalence.
