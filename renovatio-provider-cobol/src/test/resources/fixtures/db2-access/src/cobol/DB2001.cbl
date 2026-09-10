       IDENTIFICATION DIVISION.
       PROGRAM-ID. DB2001.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-EMP-ID          PIC S9(4) COMP VALUE 0.
       01  WS-EMP-NAME        PIC X(50).
       01  WS-EMP-DEPT        PIC X(20).
       01  WS-EMP-SALARY      PIC S9(8)V99 COMP-3 VALUE 0.
       01  WS-RESPONSE        PIC S9(8) COMP VALUE 0.
       01  WS-MESSAGE         PIC X(80).

       COPY EMP-REC.

       PROCEDURE DIVISION.
       MAIN-LOGIC.
           PERFORM INITIALIZE
           PERFORM FETCH-EMPLOYEE
           PERFORM UPDATE-SALARY
           PERFORM DISPLAY-RESULT
           PERFORM CLEANUP
           STOP RUN.

       INITIALIZE.
           MOVE 0 TO WS-EMP-ID
           MOVE SPACES TO WS-EMP-NAME
           MOVE SPACES TO WS-EMP-DEPT
           MOVE 0 TO WS-EMP-SALARY
           MOVE 0 TO WS-RESPONSE
           MOVE SPACES TO WS-MESSAGE.

       FETCH-EMPLOYEE.
           MOVE WS-EMP-ID TO EMP-ID IN EMPLOYEE-RECORD
           EXEC SQL
               SELECT EMP_NAME, EMP_DEPT, EMP_SALARY
               INTO :WS-EMP-NAME, :WS-EMP-DEPT, :WS-EMP-SALARY
               FROM EMPLOYEES
               WHERE EMP_ID = :WS-EMP-ID
           END-EXEC
           IF SQLCODE = 0 THEN
               MOVE WS-EMP-NAME TO EMP-NAME IN EMPLOYEE-RECORD
               MOVE WS-EMP-DEPT TO EMP-DEPT IN EMPLOYEE-RECORD
               MOVE WS-EMP-SALARY TO EMP-SALARY IN EMPLOYEE-RECORD
               STRING 'Employee: ' DELIMITED BY SIZE
                      WS-EMP-NAME DELIMITED BY SIZE
                      INTO WS-MESSAGE
           ELSE
               MOVE 'Employee not found' TO WS-MESSAGE
           END-IF.

       UPDATE-SALARY.
           COMPUTE WS-EMP-SALARY = WS-EMP-SALARY * 1.05
           EXEC SQL
               UPDATE EMPLOYEES
               SET EMP_SALARY = :WS-EMP-SALARY
               WHERE EMP_ID = :WS-EMP-ID
           END-EXEC
           IF SQLCODE = 0 THEN
               MOVE 'Salary updated successfully' TO WS-MESSAGE
           ELSE
               MOVE 'Error updating salary' TO WS-MESSAGE
           END-IF.

       DISPLAY-RESULT.
           DISPLAY WS-MESSAGE.

       CLEANUP.
           MOVE 0 TO WS-EMP-ID
           MOVE SPACES TO WS-EMP-NAME
           MOVE SPACES TO WS-EMP-DEPT
           MOVE 0 TO WS-EMP-SALARY
           MOVE 0 TO WS-RESPONSE
           MOVE SPACES TO WS-MESSAGE.
