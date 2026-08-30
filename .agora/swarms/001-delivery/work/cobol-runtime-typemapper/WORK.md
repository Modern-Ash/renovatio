---
schema: "agora/work/v1"
id: "cobol-runtime-typemapper"
swarm: "delivery"
title: "COBOL runtime + rich PIC type model"
state: "verifying"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"pictype-parse":"PicClause parser produces PicType with digits, scale, signed and usage (DISPLAY/COMP/COMP-3/COMP-5) for representative PIC clauses","decimal-semantics":"CobolDecimal reproduces COBOL truncation, ROUNDED and ON SIZE ERROR for a fixture set","move-semantics":"CobolMove reproduces numeric truncation and alphanumeric padding/truncation","typemapper-compat":"CobolTypeMapper.picToJavaType keeps current outputs; new picType() API added; existing IR tests stay green","tests-first":"Every new class has a test that was seen to fail before implementation; mvn test green"}
satisfied-criteria: []
criterion-statuses: {"pictype-parse":["specified","planned","implemented","verified"],"decimal-semantics":["specified","planned","implemented","verified"],"move-semantics":["specified","planned","implemented","verified"],"typemapper-compat":["specified","planned","implemented","verified"],"tests-first":["specified","planned","implemented","verified"]}
required-artifacts: ["implementation-plan"]
child-work-refs: []
budget-limits: null
---

# COBOL runtime + rich PIC type model

## Description

New renovatio-cobol-runtime module (fixed-point decimal with COBOL truncation/ROUNDED/ON SIZE ERROR, MOVE numeric+alphanumeric semantics, EBCDIC collation) and a rich PicType descriptor (digits, scale, signed, usage) replacing the string-only CobolTypeMapper. Semantic base for faithful COBOL->Java/Python transliteration.

## Acceptance criteria

- [ ] **pictype-parse:** PicClause parser produces PicType with digits, scale, signed and usage (DISPLAY/COMP/COMP-3/COMP-5) for representative PIC clauses; stages: specified, planned, implemented, verified
- [ ] **decimal-semantics:** CobolDecimal reproduces COBOL truncation, ROUNDED and ON SIZE ERROR for a fixture set; stages: specified, planned, implemented, verified
- [ ] **move-semantics:** CobolMove reproduces numeric truncation and alphanumeric padding/truncation; stages: specified, planned, implemented, verified
- [ ] **typemapper-compat:** CobolTypeMapper.picToJavaType keeps current outputs; new picType() API added; existing IR tests stay green; stages: specified, planned, implemented, verified
- [ ] **tests-first:** Every new class has a test that was seen to fail before implementation; mvn test green; stages: specified, planned, implemented, verified

## Required artifacts

- implementation-plan
