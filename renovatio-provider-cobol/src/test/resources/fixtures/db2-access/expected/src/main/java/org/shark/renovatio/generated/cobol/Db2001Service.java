package org.shark.renovatio.generated.cobol;

/**
 * Service interface for COBOL program: Db2001
 */
public interface Db2001Service {
  /**
   * Process the COBOL program logic with given input
   * @param input Input data structure
   * @return Processed output data structure
   */
  Db2001DTO process(Db2001DTO input);

  /**
   * Validate input data structure
   * @param input Input data to validate
   * @return true if valid, false otherwise
   */
  boolean validate(Db2001DTO input);
}
