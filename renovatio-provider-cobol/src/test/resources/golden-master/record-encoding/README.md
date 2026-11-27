# Golden master: record encoding

`BINREC.cbl` writes two records whose fields cover every encoding the generated DTO record
accessors support: binary (`COMP`, `BINARY`, `PIC 9` and `PIC S9`, 2/4/8 bytes), packed decimal
(`COMP-3`, signed and unsigned), signed zoned decimal (`DISPLAY`) and alphanumeric text.
`expected-records.bin` is GnuCOBOL's output, unchanged.

```sh
cobc -x -std=ibm -fsign=EBCDIC BINREC.cbl -o binrec
OUTFILE=expected-records.bin ./binrec
```

`-std=ibm` gives IBM binary sizes and big-endian binary. `-fsign=EBCDIC` gives the IBM overpunch
sign (`{`, `A`-`I`, `}`, `J`-`R`), the convention used by the CardDemo ASCII data files.

`COMP-5` is left out on purpose. GnuCOBOL stores it in the host byte order (little-endian on x86).
The generated code uses big-endian, as on z/Architecture.
