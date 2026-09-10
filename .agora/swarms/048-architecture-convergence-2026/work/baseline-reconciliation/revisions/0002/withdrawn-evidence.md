# Revision 2 withdrawn evidence

## `issue-222-pr-ci-r2`

This evidence was withdrawn after PR review found that its Tool Run predated
the commit declared by `tested-commit`. It remains recorded here for audit but
is not active evidence for revision 2.

```yaml
id: issue-222-pr-ci-r2
type: ci
phase: review-revalidation
result: success
revision: 2
artifact-references: [repo://.agora/tool-runs/tool-20260908t23051788919519z/RESULT.md]
artifact-content-sha256: efa5bea45dc7ddf35d5414898363cde3de6c46936acb17c6f7d59c327498b1bf
produced-by: project:agent
timestamp: 2026-09-08T23:27:38.627541Z
tested-commit: b765b20e2440aeff99cba1010c239fb2c48dc841
command: [gh, pr, checks, 239]
exit-code: 0
environment: github-actions
```

Replacement evidence: `issue-222-pr-ci-caf23cb8`, backed by Tool Run
`tool-20260908t23361788921393z`, executed after commit
`caf23cb88f6dd651a3568395647eebcdc975d399` completed CI.
