       IDENTIFICATION DIVISION.
       PROGRAM-ID. BATCH001.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-COUNTER         PIC S9(4) COMP VALUE 0.
       01  WS-TOTAL           PIC S9(8) COMP VALUE 0.
       01  WS-RESULT          PIC S9(8) COMP VALUE 0.
       01  WS-FLAG            PIC X VALUE 'N'.
       01  WS-OUTPUT          PIC X(80).

       PROCEDURE DIVISION.
       MAIN-LOGIC.
           PERFORM INITIALIZE
           PERFORM PROCESS-DATA
           PERFORM DISPLAY-RESULT
           PERFORM CLEANUP
           STOP RUN.

       INITIALIZE.
           MOVE 0 TO WS-COUNTER
           MOVE 0 TO WS-TOTAL
           MOVE 0 TO WS-RESULT
           MOVE 'N' TO WS-FLAG.

       PROCESS-DATA.
           PERFORM VARYING WS-COUNTER FROM 1 BY 1
               UNTIL WS-COUNTER > 10
               COMPUTE WS-RESULT = WS-COUNTER * 2
               ADD WS-RESULT TO WS-TOTAL
               IF WS-TOTAL > 50 THEN
                   MOVE 'Y' TO WS-FLAG
               END-IF
           END-PERFORM.

       DISPLAY-RESULT.
           IF WS-FLAG = 'Y' THEN
               MOVE 'Total exceeded 50' TO WS-OUTPUT
           ELSE
               MOVE 'Total within limit' TO WS-OUTPUT
           END-IF
           DISPLAY WS-OUTPUT.

       CLEANUP.
           MOVE 0 TO WS-COUNTER
           MOVE 0 TO WS-TOTAL
           MOVE 0 TO WS-RESULT.
