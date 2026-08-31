package org.shark.renovatio.generated.cobol;

/**
 * Service interface for COBOL program: Prog3
 */
public interface Prog3Service {
  /**
   * Process the COBOL program logic with given input
   * @param input Input data structure
   * @return Processed output data structure
   */
  Prog3DTO process(Prog3DTO input);

  /**
   * Validate input data structure
   * @param input Input data to validate
   * @return true if valid, false otherwise
   */
  boolean validate(Prog3DTO input);
}
