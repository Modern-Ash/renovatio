       IDENTIFICATION DIVISION.
       PROGRAM-ID. USERMNT.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-USRSEC-FILE       PIC X(08) VALUE 'USRSEC  '.
       01  WS-RESP-CD           PIC S9(09) COMP VALUE ZEROS.
       01  WS-OUT.
           05 WS-OUT-RESP       PIC 9(02).
           05 FILLER            PIC X(01) VALUE '|'.
           05 WS-OUT-REC        PIC X(16).
       01  WS-REQUEST.
           05 RQ-ACTION         PIC X(01).
           05 RQ-USER-ID        PIC X(08).
           05 RQ-NAME           PIC X(08).
       01  SEC-USER-DATA.
           05 SEC-USR-ID        PIC X(08).
           05 SEC-USR-NAME      PIC X(08).
       PROCEDURE DIVISION.
       MAIN-PARA.
           MOVE DFHCOMMAREA(1:EIBCALEN) TO WS-REQUEST
           MOVE SPACES TO WS-OUT-REC
           EVALUATE RQ-ACTION
              WHEN 'R'
                 PERFORM READ-USER
                 IF WS-RESP-CD = DFHRESP(NORMAL)
                    MOVE SEC-USER-DATA TO WS-OUT-REC
                 END-IF
              WHEN 'W'
                 MOVE RQ-USER-ID TO SEC-USR-ID
                 MOVE RQ-NAME    TO SEC-USR-NAME
                 EXEC CICS WRITE
                      DATASET   (WS-USRSEC-FILE)
                      FROM      (SEC-USER-DATA)
                      LENGTH    (LENGTH OF SEC-USER-DATA)
                      RIDFLD    (SEC-USR-ID)
                      KEYLENGTH (LENGTH OF SEC-USR-ID)
                      RESP      (WS-RESP-CD)
                 END-EXEC
              WHEN 'U'
                 PERFORM READ-USER
                 MOVE RQ-NAME TO SEC-USR-NAME
                 EXEC CICS REWRITE
                      DATASET   (WS-USRSEC-FILE)
                      FROM      (SEC-USER-DATA)
                      RESP      (WS-RESP-CD)
                 END-EXEC
              WHEN 'D'
                 PERFORM READ-USER
                 EXEC CICS DELETE
                      DATASET   (WS-USRSEC-FILE)
                      RESP      (WS-RESP-CD)
                 END-EXEC
              WHEN 'N'
                 PERFORM START-BROWSE
                 IF WS-RESP-CD = DFHRESP(NORMAL)
                    EXEC CICS READNEXT DATASET(WS-USRSEC-FILE)
                         INTO(SEC-USER-DATA) RIDFLD(RQ-USER-ID)
                         RESP(WS-RESP-CD) END-EXEC
                    EXEC CICS READNEXT DATASET(WS-USRSEC-FILE)
                         INTO(SEC-USER-DATA) RIDFLD(RQ-USER-ID)
                         RESP(WS-RESP-CD) END-EXEC
                    PERFORM END-BROWSE
                 END-IF
              WHEN 'P'
                 PERFORM START-BROWSE
                 EXEC CICS READPREV DATASET(WS-USRSEC-FILE)
                      INTO(SEC-USER-DATA) RIDFLD(RQ-USER-ID)
                      RESP(WS-RESP-CD) END-EXEC
                 EXEC CICS READPREV DATASET(WS-USRSEC-FILE)
                      INTO(SEC-USER-DATA) RIDFLD(RQ-USER-ID)
                      RESP(WS-RESP-CD) END-EXEC
                 PERFORM END-BROWSE
           END-EVALUATE
           MOVE WS-RESP-CD TO WS-OUT-RESP
           EXEC CICS SEND TEXT FROM(WS-OUT) END-EXEC
           EXEC CICS RETURN END-EXEC.
       READ-USER.
           MOVE RQ-USER-ID TO SEC-USR-ID
           EXEC CICS READ
                DATASET   (WS-USRSEC-FILE)
                INTO      (SEC-USER-DATA)
                LENGTH    (LENGTH OF SEC-USER-DATA)
                RIDFLD    (SEC-USR-ID)
                KEYLENGTH (LENGTH OF SEC-USR-ID)
                UPDATE
                RESP      (WS-RESP-CD)
           END-EXEC.
       START-BROWSE.
           EXEC CICS STARTBR DATASET(WS-USRSEC-FILE)
                RIDFLD(RQ-USER-ID) RESP(WS-RESP-CD) END-EXEC.
       END-BROWSE.
           IF WS-RESP-CD = DFHRESP(NORMAL)
              MOVE SEC-USER-DATA TO WS-OUT-REC
           END-IF
           EXEC CICS ENDBR DATASET(WS-USRSEC-FILE) END-EXEC.
