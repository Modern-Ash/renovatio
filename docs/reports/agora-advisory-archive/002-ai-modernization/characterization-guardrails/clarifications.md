---
schema: "agora/clarifications/v1"
swarm: "ai-modernization"
work: "characterization-guardrails"
created-at: "2026-08-30T14:48:47.092464Z"
last-run-input-sha256: "b5153c8d9e2a29c0a32f1266cf4a4b5f69a17743696679a98af2f3173b771eed"
last-run-question-count: 0
last-run-unanswered-count: 0
last-run-by: "project:owner"
last-run-at: "2026-08-30T15:05:02.661958Z"
---

# Clarifications for characterization-guardrails

| Question | Answer | Actor | Timestamp | Input SHA-256 |
| --- | --- | --- | --- | --- |
| What exact `spec` artifact must be registered, and what criteria determine that it is complete enough for the Spec Owner to move the work from `drafting` to `clarified`? |  | project:owner | 2026-08-30T14:48:47.092464Z | 4a2756d7fc64daa1668131b48cbb1cd95fec83d0d17d25eb57608edac147cd18 |
| Which representative COBOL programs and advanced residual constructs must be included in the committed golden-fixture corpus, with what expected outputs? |  | project:owner | 2026-08-30T14:48:47.092464Z | 4a2756d7fc64daa1668131b48cbb1cd95fec83d0d17d25eb57608edac147cd18 |
| What schema, compiler/toolchain versions, and characterization-test commands define successful completion of the first four ordered gates? |  | project:owner | 2026-08-30T14:48:47.092464Z | 4a2756d7fc64daa1668131b48cbb1cd95fec83d0d17d25eb57608edac147cd18 |
| What objective checks make a proposal review-eligible, and who performs or approves that review? |  | project:owner | 2026-08-30T14:48:47.092464Z | 4a2756d7fc64daa1668131b48cbb1cd95fec83d0d17d25eb57608edac147cd18 |
| What CI environments must run offline, and what required fields and destination define a traceable manual action item when deterministic fallback occurs? |  | project:owner | 2026-08-30T14:48:47.092464Z | 4a2756d7fc64daa1668131b48cbb1cd95fec83d0d17d25eb57608edac147cd18 |
| Is `repo://docs/specs/characterization-guardrails.md` already registered as the required `spec` artifact, or must registration occur before the `spec-clarified` transition? |  | project:owner | 2026-08-30T14:56:06.247785Z | e1eb40f6cde0c0c67101c519f76aaa531e45ffb983245b934a13a9d2a22603d5 |
| For every failed gate, must the system always emit deterministic transliteration, or may it emit no transformed code when the specification determines that no safe fallback exists? |  | project:owner | 2026-08-30T14:56:06.247785Z | e1eb40f6cde0c0c67101c519f76aaa531e45ffb983245b934a13a9d2a22603d5 |
| Must the offline CI lane strictly block all unexpected network access, or is auditing and failing on detected access sufficient? |  | project:owner | 2026-08-30T14:56:06.247785Z | e1eb40f6cde0c0c67101c519f76aaa531e45ffb983245b934a13a9d2a22603d5 |
| Which committed JSON Schema files and versions govern the base IR, annotated sidecar, and manual action-item report at the schema-validation gate? |  | project:owner | 2026-08-30T14:56:06.247785Z | e1eb40f6cde0c0c67101c519f76aaa531e45ffb983245b934a13a9d2a22603d5 |
| Does the assigned `project:owner` satisfy the requirement that a human acting as `spec-owner` performs final review and acceptance? |  | project:owner | 2026-08-30T14:56:06.247785Z | e1eb40f6cde0c0c67101c519f76aaa531e45ffb983245b934a13a9d2a22603d5 |
| For a failed gate where no safe deterministic transliteration exists, does `safe-fallback` permit emitting no transformed code plus a manual action item, or must every failure emit a transliteration as the criterion currently states? |  | project:owner | 2026-08-30T14:57:13.288932Z | 0f9ca13cd95e27db5da72d15a3d8a7b4f00c45fc164e70066fb4f40424a817b8 |
| Which exact fixtures and expected artifacts constitute the minimum passing corpus for each family, given that `expected-ir.json` and `expected.java` are conditional on support and could otherwise leave acceptance coverage indeterminate? |  | project:owner | 2026-08-30T14:57:13.288932Z | 0f9ca13cd95e27db5da72d15a3d8a7b4f00c45fc164e70066fb4f40424a817b8 |
| What observable enforcement and test evidence must demonstrate that the offline CI lane strictly blocks unexpected network access, rather than merely running Maven offline without credentials? |  | project:owner | 2026-08-30T14:57:13.288932Z | 0f9ca13cd95e27db5da72d15a3d8a7b4f00c45fc164e70066fb4f40424a817b8 |
| Does the duplicated specification entry represent one artifact, and has `repo://docs/specs/characterization-guardrails.md` actually been registered with artifact kind `spec` for the `spec-clarified` gate? |  | project:owner | 2026-08-30T14:57:13.288932Z | 0f9ca13cd95e27db5da72d15a3d8a7b4f00c45fc164e70066fb4f40424a817b8 |
| May this specification transition to `clarified` before issue #124 delivers the annotated-model schema, with annotated output remaining categorically ineligible until that dependency is committed and active? |  | project:owner | 2026-08-30T14:57:13.288932Z | 0f9ca13cd95e27db5da72d15a3d8a7b4f00c45fc164e70066fb4f40424a817b8 |
| Must the twelve golden fixture directories and their exact committed expected-output files exist before the `spec-clarified` transition, or does defining their required coverage and file contracts satisfy this drafting gate? |  | project:owner | 2026-08-30T14:59:29.337763Z | 9ea484b0d5d8e077df66e77b9fd4ea26bcfa4880cf54566b313aa5ab17bee13b |
| Which registered content digest is the authoritative `spec` revision for `repo://docs/specs/characterization-guardrails.md`, given the repeated registrations in the governed context? |  | project:owner | 2026-08-30T14:59:29.337763Z | 9ea484b0d5d8e077df66e77b9fd4ea26bcfa4880cf54566b313aa5ab17bee13b |
| What exact rules determine whether a diff is “bounded to declared targets” and whether a public-signature change is “unexpected” at the review-eligibility gate? |  | project:owner | 2026-08-30T14:59:29.337763Z | 9ea484b0d5d8e077df66e77b9fd4ea26bcfa4880cf54566b313aa5ab17bee13b |
| Which exact Java 17 distribution/version, Maven 3.9.x patch version, and container image digest must the offline CI lane pin for acceptance? |  | project:owner | 2026-08-30T14:59:29.337763Z | 9ea484b0d5d8e077df66e77b9fd4ea26bcfa4880cf54566b313aa5ab17bee13b |
| Has `project:owner`, acting as `spec-owner`, explicitly resolved all remaining questions and accepted the authoritative spec revision for transition to `clarified`? |  | project:owner | 2026-08-30T14:59:29.337763Z | 9ea484b0d5d8e077df66e77b9fd4ea26bcfa4880cf54566b313aa5ab17bee13b |
