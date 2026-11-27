       IDENTIFICATION DIVISION.
       PROGRAM-ID. QUALEVAL.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  SCREEN-IN.
           05 USERIDI        PIC X(8).
           05 PASSWDI        PIC X(8).
       01  SCREEN-OUT REDEFINES SCREEN-IN.
           05 USERIDO        PIC X(8).
           05 PASSWDO        PIC X(8).
       01  WS-MESSAGE        PIC X(20).
       01  WS-COUNT          PIC 9(2) VALUE 0.
       PROCEDURE DIVISION.
           MOVE SPACES TO USERIDI OF SCREEN-IN
           MOVE 'SECRET' TO PASSWDI OF SCREEN-IN
           PERFORM CHECK-INPUT
           MOVE 'ADMIN' TO USERIDO OF SCREEN-OUT
           PERFORM CHECK-INPUT
           MOVE LOW-VALUES TO PASSWDI IN SCREEN-IN
           PERFORM CHECK-INPUT
           DISPLAY WS-COUNT
           GOBACK.
       CHECK-INPUT.
           EVALUATE TRUE
              WHEN USERIDI OF SCREEN-IN = SPACES OR LOW-VALUES
                 MOVE 'Please enter user' TO WS-MESSAGE
              WHEN PASSWDI OF SCREEN-IN = SPACES OR LOW-VALUES
                 MOVE 'Please enter pass' TO WS-MESSAGE
              WHEN OTHER
                 MOVE 'OK' TO WS-MESSAGE
                 ADD 1 TO WS-COUNT
           END-EVALUATE
           DISPLAY '[' WS-MESSAGE '] [' USERIDI ']'.
