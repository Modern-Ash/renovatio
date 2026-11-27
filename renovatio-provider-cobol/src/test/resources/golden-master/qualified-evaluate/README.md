# Golden master: qualified names and EVALUATE TRUE

`QUALEVAL.cbl` covers `name OF group` / `name IN group` references (including through a
REDEFINES view), `EVALUATE TRUE` with abbreviated `= SPACES OR LOW-VALUES` conditions and
`WHEN OTHER`, `MOVE LOW-VALUES`, and DISPLAY of an unsigned numeric item. `expected-stdout.txt`
is the output of GnuCOBOL 3.2:

```sh
cobc -x -std=ibm -fsign=EBCDIC QUALEVAL.cbl -o qualeval && ./qualeval > expected-stdout.txt
```
