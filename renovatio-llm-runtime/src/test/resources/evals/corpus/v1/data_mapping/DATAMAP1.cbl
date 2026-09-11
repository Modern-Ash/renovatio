IDENTIFICATION DIVISION.
       PROGRAM-ID. DATAMAP1.
       AUTHOR. RENAVOTIO EVAL.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  CUSTOMER-RECORD.
           05  CUST-ID          PIC 9(10).
           05  CUST-NAME        PIC X(30).
           05  CUST-EMAIL       PIC X(50).
           05  CUST-PHONE       PIC X(15).
           05  CUST-ADDRESS     PIC X(100).
           05  CUST-BALANCE     PIC 9(10)V99.
           05  CUST-STATUS      PIC X.
               88  ACTIVE         VALUE 'A'.
               88  INACTIVE       VALUE 'I'.

       PROCEDURE DIVISION.
       PROCESS-CUSTOMER.
           MOVE CUST-ID TO WS-ID.
           MOVE CUST-NAME TO WS-NAME.
           MOVE CUST-EMAIL TO WS-EMAIL.
           MOVE CUST-PHONE TO WS-PHONE.
           MOVE CUST-ADDRESS TO WS-ADDRESS.
           MOVE CUST-BALANCE TO WS-BALANCE.
           IF ACTIVE
               MOVE 'ACTIVE' TO WS-STATUS-DISPLAY
           ELSE
               MOVE 'INACTIVE' TO WS-STATUS-DISPLAY
           END-IF.
           CALL 'SAVE-CUSTOMER' USING WS-CUSTOMER-DTO.
           STOP RUN.