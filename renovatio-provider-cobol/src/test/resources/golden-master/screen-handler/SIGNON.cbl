       IDENTIFICATION DIVISION.
       PROGRAM-ID. SIGNON.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-TRANID            PIC X(04) VALUE 'SGN0'.
       01  WS-MESSAGE           PIC X(30) VALUE SPACES.
       01  WS-COMMAREA.
           05 CA-USER-ID        PIC X(08).
           05 CA-VISITS         PIC 9(02).
       01  SGNMAPI.
           05 USERIDI           PIC X(08).
           05 MSGI              PIC X(30).
       01  SGNMAPO REDEFINES SGNMAPI.
           05 USERIDO           PIC X(08).
           05 MSGO              PIC X(30).
       PROCEDURE DIVISION.
       MAIN-PARA.
           IF EIBCALEN = 0
              MOVE LOW-VALUES TO SGNMAPO
              PERFORM SEND-SCREEN
           ELSE
              MOVE DFHCOMMAREA(1:EIBCALEN) TO WS-COMMAREA
              EVALUATE EIBAID
                 WHEN DFHENTER
                    PERFORM PROCESS-ENTER
                 WHEN DFHPF3
                    EXEC CICS SEND TEXT FROM(WS-MESSAGE) END-EXEC
                    EXEC CICS RETURN END-EXEC
                 WHEN OTHER
                    MOVE 'Invalid key' TO WS-MESSAGE
                    PERFORM SEND-SCREEN
              END-EVALUATE
           END-IF
           EXEC CICS RETURN TRANSID(WS-TRANID) COMMAREA(WS-COMMAREA)
           END-EXEC.
       PROCESS-ENTER.
           EXEC CICS RECEIVE MAP('SGNMAP') MAPSET('SGNSET') END-EXEC
           ADD 1 TO CA-VISITS
           IF USERIDI OF SGNMAPI = SPACES OR LOW-VALUES
              MOVE 'Please enter User ID' TO WS-MESSAGE
              PERFORM SEND-SCREEN
           ELSE
              MOVE USERIDI OF SGNMAPI TO CA-USER-ID
              EXEC CICS XCTL PROGRAM('MENU') COMMAREA(WS-COMMAREA)
              END-EXEC
           END-IF.
       SEND-SCREEN.
           MOVE WS-MESSAGE TO MSGO OF SGNMAPO
           EXEC CICS SEND MAP('SGNMAP') MAPSET('SGNSET') FROM(SGNMAPO)
           END-EXEC.
