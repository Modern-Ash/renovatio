package org.shark.renovatio.generated.cobol;

/**
 * Service interface for COBOL program: Samplecopybook
 */
public interface SamplecopybookService {
  /**
   * Process the COBOL program logic with given input
   * @param input Input data structure
   * @return Processed output data structure
   */
  SamplecopybookDTO process(SamplecopybookDTO input);

  /**
   * Validate input data structure
   * @param input Input data to validate
   * @return true if valid, false otherwise
   */
  boolean validate(SamplecopybookDTO input);
}
