       IDENTIFICATION DIVISION.
       PROGRAM-ID. FUNCS.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-NAME              PIC X(12) VALUE '  Ann Smith '.
       01  WS-OUT               PIC X(12).
       01  WS-LEN               PIC 9(03).
       01  WS-AMT-TXT           PIC X(10) VALUE ' 1,234.50-'.
       01  WS-NUM-TXT           PIC X(06) VALUE '  42  '.
       01  WS-BAD-TXT           PIC X(06) VALUE '4X2   '.
       01  WS-AMT               PIC S9(07)V99.
       01  WS-N                 PIC 9(04).
       01  WS-T                 PIC 9(02).
       01  WS-DATE              PIC 9(08) VALUE 20240301.
       01  WS-DAYS              PIC 9(07).
       01  WS-BACK              PIC 9(08).
       01  WS-NOW               PIC X(21).
       PROCEDURE DIVISION.
       MAIN-PARA.
           MOVE FUNCTION TRIM(WS-NAME) TO WS-OUT
           DISPLAY '[' WS-OUT ']'
           MOVE FUNCTION UPPER-CASE(FUNCTION TRIM(WS-NAME)) TO WS-OUT
           DISPLAY '[' WS-OUT ']'
           MOVE FUNCTION LOWER-CASE(WS-NAME) TO WS-OUT
           DISPLAY '[' WS-OUT ']'
           MOVE FUNCTION LENGTH(FUNCTION TRIM(WS-NAME)) TO WS-LEN
           DISPLAY 'LEN ' WS-LEN
           IF FUNCTION UPPER-CASE(FUNCTION TRIM(WS-NAME)) = 'ANN SMITH'
              DISPLAY 'UPPER MATCH'
           END-IF
           COMPUTE WS-AMT = FUNCTION NUMVAL-C(WS-AMT-TXT)
           IF WS-AMT = -1234.50
              DISPLAY 'NUMVAL-C OK'
           END-IF
           MOVE FUNCTION NUMVAL(WS-NUM-TXT) TO WS-N
           DISPLAY 'NUMVAL ' WS-N
           MOVE FUNCTION TEST-NUMVAL(WS-NUM-TXT) TO WS-T
           DISPLAY 'TEST-OK ' WS-T
           IF FUNCTION TEST-NUMVAL(WS-BAD-TXT) NOT = 0
              DISPLAY 'TEST-BAD'
           END-IF
           COMPUTE WS-DAYS = FUNCTION INTEGER-OF-DATE(WS-DATE)
           DISPLAY 'DAYS ' WS-DAYS
           COMPUTE WS-BACK = FUNCTION DATE-OF-INTEGER(WS-DAYS + 1)
           DISPLAY 'NEXT ' WS-BACK
           COMPUTE WS-N = FUNCTION MOD(WS-DATE, 7)
           DISPLAY 'MOD ' WS-N
           MOVE FUNCTION CURRENT-DATE TO WS-NOW
           MOVE FUNCTION LENGTH(FUNCTION TRIM(WS-NOW)) TO WS-LEN
           DISPLAY 'NOW ' WS-LEN
           GOBACK.
