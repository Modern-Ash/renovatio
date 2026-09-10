package org.shark.renovatio.generated.cobol;

import java.lang.Override;
import org.springframework.stereotype.Service;

/**
 * Implementation of Db2001Service
 * Generated from COBOL program: Db2001
 */
@Service
public class Db2001ServiceImpl implements Db2001Service {
  @Override
  public Db2001DTO process(Db2001DTO input) {
      {
          Db2001DTO output = new Db2001DTO();
          // PERFORM INITIALIZE (paragraph not found)
          performFetchEmployee(input, output);
          performUpdateSalary(input, output);
          performDisplayResult(input, output);
          performCleanup(input, output);
          return output;
      }
  }

  @Override
  public boolean validate(Db2001DTO input) {
    if (input == null) { return false; };
    if (input.getWsEmpId() == null) { return false; };
    if (String.valueOf(Math.abs(input.getWsEmpId())).length() > 4) { return false; };
    if (input.getWsEmpName() == null || input.getWsEmpName().length() > 50) { return false; };
    if (input.getWsEmpDept() == null || input.getWsEmpDept().length() > 20) { return false; };
    if (input.getWsEmpSalary() == null) { return false; };
    if (input.getWsEmpSalary().scale() > 2) { return false; };
    if (input.getWsEmpSalary().precision() > 10) { return false; };
    if (input.getWsEmpSalary().precision() - input.getWsEmpSalary().scale() > 8) { return false; };
    if (input.getWsResponse() == null) { return false; };
    if (String.valueOf(Math.abs(input.getWsResponse())).length() > 8) { return false; };
    if (input.getWsMessage() == null || input.getWsMessage().length() > 80) { return false; };
    return true;
  }
    @GeneratedFrom(paragraph = "FETCH-EMPLOYEE", lines = "32-50")
    private void performFetchEmployee(Db2001DTO input, Db2001DTO out) {
        // COBOL not translated: EMP-ID (data item not modeled)
        // EXEC SQL SELECT EMP_NAME, EMP_DEPT, EMP_SALARY                INTO :WS-EMP-NAME, :WS-EMP-DEPT, :WS-EMP-SALARY                FROM EMPLOYEES                WHERE EMP_ID = :WS-EMP-ID
        // COBOL not translated: SQLCODE (data item not modeled)
    }
    @GeneratedFrom(paragraph = "UPDATE-SALARY", lines = "51-63")
    private void performUpdateSalary(Db2001DTO input, Db2001DTO out) {
        // COBOL not translated: WS, EMP, SALARY (data item not modeled)
        // EXEC SQL UPDATE EMPLOYEES                SET EMP_SALARY = :WS-EMP-SALARY                WHERE EMP_ID = :WS-EMP-ID
        // COBOL not translated: SQLCODE (data item not modeled)
    }
    @GeneratedFrom(paragraph = "DISPLAY-RESULT", lines = "64-66")
    private void performDisplayResult(Db2001DTO input, Db2001DTO out) {
        System.out.println(out.getWsMessage());
    }
    @GeneratedFrom(paragraph = "CLEANUP", lines = "67-73")
    private void performCleanup(Db2001DTO input, Db2001DTO out) {
        out.setWsEmpId(0);
        // COBOL not translated: SPACES (data item not modeled)
        // COBOL not translated: SPACES (data item not modeled)
        out.setWsEmpSalary(new java.math.BigDecimal("0"));
        out.setWsResponse(0);
        // COBOL not translated: SPACES (data item not modeled)
    }
}@interface GeneratedFrom {
    String paragraph();
    String lines();
}
