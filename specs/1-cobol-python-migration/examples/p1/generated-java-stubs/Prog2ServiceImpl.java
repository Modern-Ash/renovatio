package org.shark.renovatio.generated.cobol;

import java.lang.Override;
import org.springframework.stereotype.Service;

/**
 * Implementation of Prog2Service
 * Generated from COBOL program: Prog2
 */
@Service
public class Prog2ServiceImpl implements Prog2Service {
  @Override
  public Prog2DTO process(Prog2DTO input) {
    // TODO: Implement COBOL business logic;
    // Original COBOL program: Prog2;
    Prog2DTO output = new Prog2DTO();
    return output;
  }

  @Override
  public boolean validate(Prog2DTO input) {
    if (input == null) { return false; };
    if (input.getField1() == null || input.getField1().length() > 5) { return false; };
    if (input.getField2() == null) { return false; };
    if (input.getField2() < 0) { return false; };
    if (String.valueOf(Math.abs(input.getField2())).length() > 5) { return false; };
    if (input.getField1b() == null || input.getField1b().length() > 3) { return false; };
    return true;
  }
}
