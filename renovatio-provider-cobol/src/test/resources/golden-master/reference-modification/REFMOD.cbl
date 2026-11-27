       IDENTIFICATION DIVISION.
       PROGRAM-ID. REFMOD.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-TEXT          PIC X(10) VALUE 'ABCDEFGHIJ'.
       01  WS-PART          PIC X(4).
       01  WS-POS           PIC 9(2) VALUE 3.
       01  WS-LEN           PIC 9(2) VALUE 2.
       01  WS-CODE          PIC X(3).
           88 CODE-OK       VALUE 'OK'.
       01  WS-REC.
           05 WS-A          PIC X(3).
           05 WS-B          PIC X(5).
       PROCEDURE DIVISION.
           MOVE WS-TEXT(2:3) TO WS-PART
           DISPLAY '[' WS-PART ']'
           MOVE WS-TEXT(WS-POS:WS-LEN) TO WS-PART
           DISPLAY '[' WS-PART ']'
           MOVE WS-TEXT(8:) TO WS-PART
           DISPLAY '[' WS-PART ']'
           MOVE 'xyz' TO WS-TEXT(4:2)
           DISPLAY '[' WS-TEXT ']'
           MOVE 'Q' TO WS-TEXT(WS-POS:WS-LEN)
           DISPLAY '[' WS-TEXT ']'
           MOVE 'OK' TO WS-CODE
           DISPLAY '[' WS-CODE ']'
           IF WS-CODE = 'OK'
              DISPLAY 'EQ-LITERAL'
           END-IF
           IF CODE-OK
              DISPLAY 'LEVEL-88'
           END-IF
           EVALUATE WS-CODE
              WHEN 'OK'
                 DISPLAY 'EVALUATE-OK'
              WHEN OTHER
                 DISPLAY 'EVALUATE-OTHER'
           END-EVALUATE
           MOVE 'TOOLONGVALUE' TO WS-A
           MOVE 'ab' TO WS-B
           DISPLAY '[' WS-REC ']'
           MOVE WS-REC(2:4) TO WS-PART
           DISPLAY '[' WS-PART ']'
           GOBACK.
