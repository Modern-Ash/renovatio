       01  EMPLOYEE-RECORD.
           05  EMP-ID              PIC S9(4) COMP.
           05  EMP-NAME            PIC X(50).
           05  EMP-DEPT            PIC X(20).
           05  EMP-SALARY          PIC S9(8)V99 COMP-3.
           05  EMP-HIRE-DATE       PIC X(10).
           05  EMP-STATUS          PIC X(1).
               88  ACTIVE          VALUE 'A'.
               88  INACTIVE        VALUE 'I'.
               88  TERMINATED      VALUE 'T'.
