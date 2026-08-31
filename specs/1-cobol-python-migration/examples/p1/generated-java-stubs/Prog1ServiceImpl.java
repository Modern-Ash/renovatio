package org.shark.renovatio.generated.cobol;

import java.lang.Override;
import org.springframework.stereotype.Service;

/**
 * Implementation of Prog1Service
 * Generated from COBOL program: Prog1
 */
@Service
public class Prog1ServiceImpl implements Prog1Service {
  @Override
  public Prog1DTO process(Prog1DTO input) {
    // TODO: Implement COBOL business logic;
    // Original COBOL program: Prog1;
    Prog1DTO output = new Prog1DTO();
    return output;
  }

  @Override
  public boolean validate(Prog1DTO input) {
    if (input == null) { return false; };
    if (input.getField1() == null || input.getField1().length() > 10) { return false; };
    if (input.getAmount() == null) { return false; };
    if (input.getAmount().signum() < 0) { return false; };
    if (input.getAmount().scale() > 2) { return false; };
    if (input.getAmount().precision() > 9) { return false; };
    if (input.getAmount().precision() - input.getAmount().scale() > 7) { return false; };
    return true;
  }
}
