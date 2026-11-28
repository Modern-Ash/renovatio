       IDENTIFICATION DIVISION.
       PROGRAM-ID. EDITS.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-NEG               PIC S9(09)V99 VALUE -1234.5.
       01  WS-POS               PIC S9(09)V99 VALUE 1234567.89.
       01  WS-ZERO              PIC S9(09)V99 VALUE 0.
       01  WS-SMALL             PIC 9(04)V99 VALUE 12.3.
       01  WS-INT               PIC 9(05) VALUE 42.
       01  WS-BIG               PIC 9(08) VALUE 12345678.
       01  E1                   PIC +ZZZ,ZZZ,ZZZ.99.
       01  E2                   PIC ZZZ9.
       01  E3                   PIC ZZ,ZZ9.99-.
       01  E4                   PIC $$$,$$9.99.
       01  E5                   PIC ***,**9.99.
       01  E6                   PIC -9(7).99.
       01  E7                   PIC Z(5)9CR.
       01  E8                   PIC ZZZZ.ZZ.
       01  E9                   PIC ++++9.
       01  E10                  PIC 9(4).
       PROCEDURE DIVISION.
       MAIN-PARA.
           MOVE WS-NEG TO E1
           DISPLAY '[' E1 ']'
           MOVE WS-POS TO E1
           DISPLAY '[' E1 ']'
           MOVE WS-ZERO TO E1
           DISPLAY '[' E1 ']'
           MOVE WS-ZERO TO E2
           DISPLAY '[' E2 ']'
           MOVE WS-INT TO E2
           DISPLAY '[' E2 ']'
           MOVE WS-NEG TO E3
           DISPLAY '[' E3 ']'
           MOVE WS-POS TO E3
           DISPLAY '[' E3 ']'
           MOVE WS-SMALL TO E4
           DISPLAY '[' E4 ']'
           MOVE WS-ZERO TO E4
           DISPLAY '[' E4 ']'
           MOVE WS-SMALL TO E5
           DISPLAY '[' E5 ']'
           MOVE WS-ZERO TO E5
           DISPLAY '[' E5 ']'
           MOVE WS-NEG TO E6
           DISPLAY '[' E6 ']'
           MOVE WS-NEG TO E7
           DISPLAY '[' E7 ']'
           MOVE WS-INT TO E7
           DISPLAY '[' E7 ']'
           MOVE WS-ZERO TO E8
           DISPLAY '[' E8 ']'
           MOVE WS-SMALL TO E8
           DISPLAY '[' E8 ']'
           MOVE WS-NEG TO E9
           DISPLAY '[' E9 ']'
           MOVE WS-INT TO E9
           DISPLAY '[' E9 ']'
           MOVE WS-BIG TO E2
           DISPLAY '[' E2 ']'
           GOBACK.
