# Implementation plan

1. Add a compact JPA entity and service keyed by project id, validating the two allowed fields.
2. Expose authorized context endpoints through the existing Workbench controller.
3. Make Theia restore a live context after project authorization and persist project/area/asset
   changes with local storage retained only as a non-authoritative fallback.
4. Test validation, recovery and the browser contract; document local integration evidence.
