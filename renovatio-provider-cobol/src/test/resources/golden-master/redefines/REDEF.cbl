       IDENTIFICATION DIVISION.
       PROGRAM-ID. REDEF.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-MESSAGE.
           02 WS-SEVERITY   PIC X(04).
           02 WS-SEVERITY-N REDEFINES WS-SEVERITY PIC 9(4).
           02 FILLER        PIC X(06) VALUE ' code:'.
       01  WS-DATE-AREA.
           05 WS-DATE       PIC X(8).
       01  WS-DATE-PARTS REDEFINES WS-DATE-AREA.
           05 WS-YYYY       PIC 9(4).
           05 WS-MM         PIC X(2).
           05 WS-DD         PIC X(2).
       01  WS-COPY          PIC X(8).
       PROCEDURE DIVISION.
           MOVE 42 TO WS-SEVERITY-N
           DISPLAY WS-SEVERITY
           DISPLAY WS-MESSAGE
           MOVE 'ABCD' TO WS-SEVERITY
           DISPLAY WS-MESSAGE
           MOVE '20261126' TO WS-DATE
           DISPLAY WS-MM
           DISPLAY WS-DD
           MOVE '07' TO WS-MM
           MOVE WS-DATE TO WS-COPY
           DISPLAY WS-COPY
           MOVE 1999 TO WS-YYYY
           DISPLAY WS-DATE-AREA
           GOBACK.
