       IDENTIFICATION DIVISION.
       PROGRAM-ID. GOTOS.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-N                 PIC 9(02) VALUE 0.
       01  WS-I                 PIC 9(02) VALUE 0.
       01  WS-TOTAL             PIC 9(04) VALUE 0.
       PROCEDURE DIVISION.
       0000-MAIN.
           PERFORM 1000-CHECK
              THRU 1000-CHECK-EXIT
           MOVE 5 TO WS-N
           PERFORM 1000-CHECK
              THRU 1000-CHECK-EXIT
           PERFORM 2000-SUM THRU 2000-SUM-EXIT
           DISPLAY 'TOTAL ' WS-TOTAL
           IF WS-TOTAL > 10
              GO TO 9999-END
           END-IF
           DISPLAY 'NOT REACHED'.
       1000-CHECK.
           IF WS-N = 0
              DISPLAY 'N IS ZERO'
              GO TO 1000-CHECK-EXIT
           END-IF
           DISPLAY 'N IS ' WS-N.
       1000-CHECK-EXIT.
           EXIT.
       2000-SUM.
           PERFORM VARYING WS-I FROM 1 BY 1 UNTIL WS-I > 9
              ADD WS-I TO WS-TOTAL
              IF WS-TOTAL > 12
                 GO TO 2000-SUM-EXIT
              END-IF
           END-PERFORM
           DISPLAY 'LOOP DONE'.
       2000-SUM-EXIT.
           EXIT.
       9999-END.
           DISPLAY 'END ' WS-I
           STOP RUN.
