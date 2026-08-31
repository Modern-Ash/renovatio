# Test report: review-only idiomatic polish proposals

> GitHub issue: [#128](https://github.com/Modern-Ash/renovatio/issues/128)  
> Agora work: `ai-modernization/idiomatic-polish-proposals`  
> Implementation commit: `247444eb56d2ce160b5e13e738a65d21e38426bb`

## Environment

The authoritative lane used the pinned image
`docker.io/library/maven:3.9.12-eclipse-temurin-17@sha256:a0603aab698040d9c94259f379ec0487da1678560748d6c7508483034033c53d`
(`linux/amd64` image ID
`sha256:897d798fb52cd9312a97f1e3542c0f00acb9ba6a700d7ab734da0ab148c38dcf`).
It reported Maven 3.9.12 and Eclipse Adoptium Java 17.0.18. Docker ran with
`--network none`, did not receive provider credentials, and mounted the existing Maven repository
read-only.

## Commands

Focused contract lane:

```text
mvn -q -pl renovatio-provider-cobol -am clean test -o -Djacoco.skip=true \
  -Dtest=PolishContractsTest,IdiomaticPolishServiceTest,PolishSchemaTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Full affected reactor on the host, followed by the authoritative pinned offline lane:

```text
mvn -q -pl renovatio-provider-cobol,cobol-openrewrite-recipes -am clean test -o \
  -Djacoco.skip=true

docker run --rm --network none \
  -v <clean-copy>:/workspace -v <maven-cache>:/root/.m2:ro -w /workspace \
  maven@sha256:a0603aab698040d9c94259f379ec0487da1678560748d6c7508483034033c53d \
  sh -lc 'mvn -version && mvn -B -o \
    -pl renovatio-provider-cobol,cobol-openrewrite-recipes -am clean test \
    -Djacoco.skip=true'
```

## Results

All commands passed. The focused lane ran 15 polish contract tests with no failures, errors, or
skips. The network-disabled Java 17 reactor produced these totals:

| Module | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| `renovatio-shared` | 23 | 0 | 0 | 0 |
| `renovatio-core` | 31 | 0 | 0 | 0 |
| `renovatio-provider-java` | 13 | 0 | 0 | 0 |
| `renovatio-cobol-runtime` | 23 | 0 | 0 | 0 |
| `renovatio-cobol-ir` | 55 | 0 | 0 | 0 |
| `renovatio-cobol-annotations` | 2 | 0 | 0 | 0 |
| `cobol-openrewrite-recipes` | 24 | 0 | 0 | 0 |
| `renovatio-provider-cobol` | 78 | 0 | 0 | 0 |
| **Total** | **249** | **0** | **0** | **0** |

## Acceptance evidence

| Obligation | Evidence |
| --- | --- |
| `diff-only` | Success emitted exactly `proposal.patch` and `manifest.json`; a repeated identical request produced byte-identical files and the same content-addressed directory. Reflection over the public service API found `propose` and no apply, accept, commit, or merge operation. |
| `eligible-only` | Forced red schema, compilation, characterization, reproducibility, and unresolved-error prerequisites produced five ineligible outcomes, zero generator calls, and no patch directory. |
| Ordered candidate gates | Forced schema, compilation, characterization, and review-eligibility failures stopped at the first failed gate. An undeclared diff path failed the built-in schema before the injected schema adapter ran. |
| `human-gate` | All four closed families retained `reviewState=PROPOSED` and `disposition=ELIGIBLE_FOR_REVIEW`; an unapproved public-signature change failed review eligibility. |
| `discard-on-failure` | Every forced candidate failure retained no artifact directory and wrote the stable current-run action report. A generator exception produced the same action-item ID on repetition. Disk-only stale action items were not merged into the new report. |
| Source retention | The declared generated Java file remained byte-identical across a successful proposal and the reported original and retained generated-tree hashes matched. Candidate validation received immutable bounded inputs; only the report writer was reachable from the service. |
| Family closure | Domain naming, port extraction, strategy extraction, and flag collapse each passed their strict family contract and the closed manifest schema. Invalid identifier, dependency/config addition, non-exhaustive strategy, and non-bijective flag states failed closed. |
| Provenance | Candidate construction required successful governed provenance: prompt ID/version/hash, output-schema hash, validators, cache key/envelope hash, provider/model, Agora run, and `MODEL_SUCCESS`. |

## Reproducibility identities

The successful fixture used these stable lowercase SHA-256 identities:

- generated input: `f99776efea5ddbc64339017ba5b909376d7f6cc20d325ccfb545db3e67479680`;
- accepted semantic projection: `0015d0b4ddc27a524bf1e833f529b84637e3348faa196de3369c505133c12b6a`;
- normalized proposal patch: `8ac76b479353aaf60ac7427eef93fd735d07fae3a094849321e27b2c20daaeb7`;
- generated-tree snapshot before and after:
  `6bcd330a7b5a7c250ad4ea6fa12cf7bb61c5796e9191c043b7860054b75221ad`.

The retained manifest also binds the exact selector `move-numeric`, its behavior hash, the
repository/baseline references, Java/Maven versions, declared path/node selector maps, and all four
executed gates. No network access or provider credential was required by either verification lane.
