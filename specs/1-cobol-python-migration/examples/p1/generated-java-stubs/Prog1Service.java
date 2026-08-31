package org.shark.renovatio.generated.cobol;

/**
 * Service interface for COBOL program: Prog1
 */
public interface Prog1Service {
  /**
   * Process the COBOL program logic with given input
   * @param input Input data structure
   * @return Processed output data structure
   */
  Prog1DTO process(Prog1DTO input);

  /**
   * Validate input data structure
   * @param input Input data to validate
   * @return true if valid, false otherwise
   */
  boolean validate(Prog1DTO input);
}
