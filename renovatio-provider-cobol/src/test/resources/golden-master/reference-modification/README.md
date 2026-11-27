# Golden master: reference modification and alphanumeric MOVE

`REFMOD.cbl` covers reading and writing `NAME(start:length)` with literal and variable positions
and an omitted length, alphanumeric MOVE padding and truncation, and alphanumeric comparisons
(relational, level-88, EVALUATE) against shorter literals. `expected-stdout.txt` is the output of
GnuCOBOL 3.2:

```sh
cobc -x -std=ibm -fsign=EBCDIC REFMOD.cbl -o refmod && ./refmod > expected-stdout.txt
```
