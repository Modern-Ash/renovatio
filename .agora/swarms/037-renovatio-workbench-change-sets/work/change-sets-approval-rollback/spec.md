# Spec

GitHub issue #183 requires Theia change sets that convert AI and generation proposals into reviewable, reversible mutations.

The implementation must provide:

- a change set manifest with file changes, decisions, evidence, and canonical hash;
- state machine: draft, review, approved, applied, rejected, rolled-back;
- mandatory diff before approval;
- role-based mutation permissions;
- exact confirmation phrases for dangerous approval, apply, and rollback;
- deterministic application through the existing workbench adapter;
- rollback to the previous generated target content;
- project history with actor, timestamp, reason, action, and manifest hash;
- UI visibility for the above boundaries.
