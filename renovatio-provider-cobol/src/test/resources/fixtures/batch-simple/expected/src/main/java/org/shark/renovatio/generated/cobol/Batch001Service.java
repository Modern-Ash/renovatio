package org.shark.renovatio.generated.cobol;

/**
 * Service interface for COBOL program: Batch001
 */
public interface Batch001Service {
  /**
   * Process the COBOL program logic with given input
   * @param input Input data structure
   * @return Processed output data structure
   */
  Batch001DTO process(Batch001DTO input);

  /**
   * Validate input data structure
   * @param input Input data to validate
   * @return true if valid, false otherwise
   */
  boolean validate(Batch001DTO input);
}
