       IDENTIFICATION DIVISION.
       PROGRAM-ID. BINREC.
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT OUT-FILE ASSIGN TO OUTFILE
                  ORGANIZATION IS SEQUENTIAL.
       DATA DIVISION.
       FILE SECTION.
       FD  OUT-FILE.
       01  OUT-REC                PIC X(32).
       WORKING-STORAGE SECTION.
       01  BIN-REC.
           05  B-SMALL            PIC S9(4) COMP.
           05  B-UNSIGNED         PIC 9(9) COMP.
           05  B-LARGE            PIC S9(18) BINARY.
           05  B-AMOUNT           PIC S9(7)V99 COMP-3.
           05  B-COUNT            PIC 9(5) COMP-3.
           05  B-ZONED            PIC S9(5)V99.
           05  B-TEXT             PIC X(3).
       PROCEDURE DIVISION.
           OPEN OUTPUT OUT-FILE
           MOVE 1234 TO B-SMALL
           MOVE 123456789 TO B-UNSIGNED
           MOVE 123456789012345678 TO B-LARGE
           MOVE 12345.67 TO B-AMOUNT
           MOVE 42 TO B-COUNT
           MOVE 321.09 TO B-ZONED
           MOVE 'ABC' TO B-TEXT
           WRITE OUT-REC FROM BIN-REC
           MOVE -1234 TO B-SMALL
           MOVE 0 TO B-UNSIGNED
           MOVE -9 TO B-LARGE
           MOVE -0.05 TO B-AMOUNT
           MOVE 0 TO B-COUNT
           MOVE -321.09 TO B-ZONED
           MOVE 'XY' TO B-TEXT
           WRITE OUT-REC FROM BIN-REC
           CLOSE OUT-FILE
           GOBACK.
