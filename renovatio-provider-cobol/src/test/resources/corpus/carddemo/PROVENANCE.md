# CardDemo corpus — provenance

Vendored subset of the **AWS Mainframe Modernization – CardDemo** sample application, used as a
fixed COBOL/JCL corpus for Renovatio's pipeline coverage measurement (GitHub issue #217).

| | |
| --- | --- |
| Upstream | https://github.com/aws-samples/aws-mainframe-modernization-carddemo |
| Upstream commit | `59cc6c2fd7ebd7ef7925cad552a01a4b8b6e4d5e` (2025-10-16) |
| License | Apache License 2.0 — see `LICENSE` and `NOTICE` |
| Copyright | Amazon.com, Inc. or its affiliates |

## Included

Source only — the subset Renovatio can consume:

- `app/cbl/` — batch + CICS online COBOL programs
- `app/cpy/`, `app/cpy-bms/` — copybooks and BMS map copybooks
- `app/jcl/` — JCL
- `app/app-transaction-type-db2/{cbl,cpy,cpy-bms,jcl}/` — DB2 subsystem
- `app/app-authorization-ims-db2-mq/{cbl,cpy,cpy-bms,jcl}/` — IMS + DB2 + MQ subsystem
- `app/app-vsam-mq/cbl/` — VSAM + MQ subsystem

## Excluded

Everything not needed for source analysis: `diagrams/`, `samples/`, `scripts/`, `data/`, `asm/`,
`maclib/`, `catlg/`, `csd/`, `bms/` (map assembler source), container/infra files, images, and
the upstream repo's own governance docs.

## Do not edit

These files are a verbatim upstream snapshot. Fixes belong upstream; to refresh, re-copy from a
newer upstream commit and update this file.
