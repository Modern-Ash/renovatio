       IDENTIFICATION DIVISION.
       PROGRAM-ID. LOADCARD.
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT IN-FILE ASSIGN TO INFILE
                  ORGANIZATION IS LINE SEQUENTIAL.
           SELECT CARD-FILE ASSIGN TO CARDFILE
                  ORGANIZATION IS INDEXED
                  ACCESS MODE IS SEQUENTIAL
                  RECORD KEY IS CARD-KEY.
       DATA DIVISION.
       FILE SECTION.
       FD  IN-FILE.
       01  IN-REC                PIC X(150).
       FD  CARD-FILE.
       01  CARD-REC.
           05 CARD-KEY           PIC X(16).
           05 CARD-DATA          PIC X(134).
       WORKING-STORAGE SECTION.
       01  WS-EOF                PIC X VALUE 'N'.
       PROCEDURE DIVISION.
           OPEN INPUT IN-FILE OUTPUT CARD-FILE
           PERFORM UNTIL WS-EOF = 'Y'
              READ IN-FILE
                 AT END MOVE 'Y' TO WS-EOF
                 NOT AT END
                    MOVE IN-REC TO CARD-REC
                    WRITE CARD-REC
              END-READ
           END-PERFORM
           CLOSE IN-FILE CARD-FILE
           GOBACK.
