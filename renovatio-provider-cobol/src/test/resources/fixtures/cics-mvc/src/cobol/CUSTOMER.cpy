       01  CUSTOMER-RECORD.
           05  CUST-ID             PIC X(10).
           05  CUST-NAME           PIC X(50).
           05  CUST-ADDRESS        PIC X(100).
           05  CUST-PHONE          PIC X(15).
           05  CUST-EMAIL          PIC X(50).
           05  CUST-BALANCE        PIC S9(8)V99 COMP-3.
           05  CUST-STATUS         PIC X(1).
               88  ACTIVE          VALUE 'A'.
               88  INACTIVE        VALUE 'I'.
               88  CLOSED          VALUE 'C'.
