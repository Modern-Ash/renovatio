IDENTIFICATION DIVISION.
       PROGRAM-ID. BUSRULE1.
       AUTHOR. RENAVOTIO EVAL.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-AGE           PIC 9(3) VALUE ZERO.
       01  WS-INCOME        PIC 9(9)V99 VALUE ZERO.
       01  WS-CREDIT-SCORE  PIC 9(3) VALUE ZERO.
       01  WS-ELIGIBLE      PIC X VALUE 'N'.

       PROCEDURE DIVISION.
       EVALUATE-ELIGIBILITY.
           IF WS-AGE >= 18 AND WS-AGE <= 65
               IF WS-INCOME >= 30000
                   IF WS-CREDIT-SCORE >= 650
                       MOVE 'Y' TO WS-ELIGIBLE
                   ELSE
                       MOVE 'N' TO WS-ELIGIBLE
                   END-IF
               ELSE
                   MOVE 'N' TO WS-ELIGIBLE
               END-IF
           ELSE
               MOVE 'N' TO WS-ELIGIBLE
           END-IF.

       CALCULATE-LIMIT.
           IF WS-ELIGIBLE = 'Y'
               COMPUTE WS-LIMIT = WS-INCOME * 0.3
           ELSE
               MOVE ZERO TO WS-LIMIT
           END-IF.

       DISPLAY-RESULT.
           DISPLAY 'ELIGIBLE: ' WS-ELIGIBLE.
           DISPLAY 'CREDIT LIMIT: ' WS-LIMIT.
           STOP RUN.