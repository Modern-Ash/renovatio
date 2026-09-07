# Implementation plan

1. Add a versioned release-hardening config that records runtime pins, distribution settings, performance budgets, security allowlists, CSP baseline and pilot fixture hashes.
2. Add scripts that validate hardening policy, performance budgets and demo pilot fixture integrity.
3. Extend CI to run API contract regressions, workbench build/test/hardening/performance/pilot/smoke, Docker build-stage validation and production audit.
4. Update workbench install, security, compatibility, verification and handoff docs.
5. Keep dashboard continuity explicit through `RENOVATIO_DASHBOARD_URL`.
