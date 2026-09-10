       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICS001.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-CUSTOMER-ID     PIC X(10).
       01  WS-CUSTOMER-NAME   PIC X(50).
       01  WS-RESPONSE        PIC S9(8) COMP.
       01  WS-MESSAGE         PIC X(80).

       COPY CUSTOMER.

       PROCEDURE DIVISION.
       MAIN-LOGIC.
           PERFORM INITIALIZE
           PERFORM RECEIVE-DATA
           PERFORM PROCESS-CUSTOMER
           PERFORM SEND-RESPONSE
           PERFORM CLEANUP
           EXEC CICS
               RETURN
           END-EXEC.

       INITIALIZE.
           MOVE SPACES TO WS-CUSTOMER-ID
           MOVE SPACES TO WS-CUSTOMER-NAME
           MOVE 0 TO WS-RESPONSE
           MOVE SPACES TO WS-MESSAGE.

       RECEIVE-DATA.
           EXEC CICS
               RECEIVE
                   INTO (WS-CUSTOMER-ID)
                   LENGTH (10)
                   RESP (WS-RESPONSE)
           END-EXEC
           IF WS-RESPONSE NOT = 0 THEN
               MOVE 'Error receiving data' TO WS-MESSAGE
               PERFORM SEND-ERROR
           END-IF.

       PROCESS-CUSTOMER.
           MOVE WS-CUSTOMER-ID TO CUST-ID IN CUSTOMER-RECORD
           EXEC CICS
               READ
                   FILE('CUSTOMERS')
                   INTO (CUSTOMER-RECORD)
                   LENGTH (LENGTH OF CUSTOMER-RECORD)
                   KEY (WS-CUSTOMER-ID)
                   RESP (WS-RESPONSE)
           END-EXEC
           IF WS-RESPONSE = 0 THEN
               MOVE CUST-NAME IN CUSTOMER-RECORD TO WS-CUSTOMER-NAME
               STRING 'Customer: ' DELIMITED BY SIZE
                      WS-CUSTOMER-NAME DELIMITED BY SIZE
                      INTO WS-MESSAGE
           ELSE
               MOVE 'Customer not found' TO WS-MESSAGE
           END-IF.

       SEND-RESPONSE.
           EXEC CICS
               SEND
                   FROM (WS-MESSAGE)
                   LENGTH (LENGTH OF WS-MESSAGE)
                   RESP (WS-RESPONSE)
           END-EXEC.

       SEND-ERROR.
           EXEC CICS
               SEND
                   FROM (WS-MESSAGE)
                   LENGTH (LENGTH OF WS-MESSAGE)
                   RESP (WS-RESPONSE)
           END-EXEC
           EXEC CICS
               RETURN
           END-EXEC.

       CLEANUP.
           MOVE SPACES TO WS-CUSTOMER-ID
           MOVE SPACES TO WS-CUSTOMER-NAME
           MOVE 0 TO WS-RESPONSE
           MOVE SPACES TO WS-MESSAGE.
