# Golden master: REDEFINES

`REDEF.cbl` writes through a base item and reads through its REDEFINES view, and the other way
round, for an elementary view (`PIC 9(4)` over `PIC X(4)`), a group view over a group, and a
view that covers part of its base. `expected-stdout.txt` is the output of GnuCOBOL 3.2:

```sh
cobc -x -std=ibm -fsign=EBCDIC REDEF.cbl -o redef && ./redef > expected-stdout.txt
```
