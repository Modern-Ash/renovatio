package org.shark.renovatio.generated.cobol;

/**
 * Service interface for COBOL program: Cics001
 */
public interface Cics001Service {
  /**
   * Process the COBOL program logic with given input
   * @param input Input data structure
   * @return Processed output data structure
   */
  Cics001DTO process(Cics001DTO input);

  /**
   * Validate input data structure
   * @param input Input data to validate
   * @return true if valid, false otherwise
   */
  boolean validate(Cics001DTO input);
}
