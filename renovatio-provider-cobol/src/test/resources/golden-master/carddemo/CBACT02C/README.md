# Golden master: CardDemo `CBACT02C`

Behavioral equivalence check for the COBOL → Java pipeline (issues #213, #216).

`CBACT02C` reads the card master file (`CARDFILE`, VSAM KSDS, sequential access) and
`DISPLAY`s every record. The Java that Renovatio generates for it must print exactly the same
bytes as the original COBOL program when both read the same records.

| File | What it is |
| --- | --- |
| `../../../corpus/carddemo/app/cbl/CBACT02C.cbl` | Program under test (vendored corpus) |
| `carddata.txt` | Input: CardDemo card data, 50 fixed 150-character records |
| `expected-stdout.txt` | Reference output of the original COBOL program |
| `LOADCARD.cbl` | Helper that loads `carddata.txt` into an indexed file for the reference run |

## How the reference output was produced

GnuCOBOL 3.2 (indexed file handler: BDB):

```sh
cobc -x -free LOADCARD.cbl -o loadcard
cobc -x -I <corpus>/app/cpy <corpus>/app/cbl/CBACT02C.cbl -o cbact02c
INFILE=carddata.txt CARDFILE=cardfile.dat ./loadcard
CARDFILE=cardfile.dat ./cbact02c > expected-stdout.txt
```

`CardDemoGoldenMasterTest` regenerates the Java from the COBOL source, compiles it, runs it
with the in-memory file adapter loaded with `carddata.txt`, and compares standard output
byte for byte. It does not need GnuCOBOL.

## Provenance

`carddata.txt` is `app/data/ASCII/carddata.txt` from
[aws-mainframe-modernization-carddemo](https://github.com/aws-samples/aws-mainframe-modernization-carddemo)
at commit `59cc6c2fd7ebd7ef7925cad552a01a4b8b6e4d5e`, the same commit as the vendored corpus.
Apache License 2.0, Copyright Amazon.com, Inc. or its affiliates — see
`../../../corpus/carddemo/LICENSE` and `NOTICE`.
