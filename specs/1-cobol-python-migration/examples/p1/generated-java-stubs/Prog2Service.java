package org.shark.renovatio.generated.cobol;

/**
 * Service interface for COBOL program: Prog2
 */
public interface Prog2Service {
  /**
   * Process the COBOL program logic with given input
   * @param input Input data structure
   * @return Processed output data structure
   */
  Prog2DTO process(Prog2DTO input);

  /**
   * Validate input data structure
   * @param input Input data to validate
   * @return true if valid, false otherwise
   */
  boolean validate(Prog2DTO input);
}
