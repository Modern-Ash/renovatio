package org.modernash.renovatio.generated.cobol;

import java.lang.String;

/**
 * Outbound port for COBOL file CUSTOMERS.
 * Records are fixed-width DISPLAY strings; every operation returns the FILE STATUS code.
 */
public interface Cics001CustomersPort {
  /**
   * Opens the file; mode is INPUT, OUTPUT, I-O or EXTEND.
   */
  String open(String mode);

  /**
   * Closes the file.
   */
  String close();

  /**
   * Reads the next record (sequential or READ NEXT).
   */
  ReadResult read();

  /**
   * Reads the record with the given key.
   */
  ReadResult read(String key);

  /**
   * Writes a new record.
   */
  String write(String record);

  /**
   * Replaces the record last read (or with the record's key).
   */
  String rewrite(String record);

  /**
   * Deletes the record with the given key.
   */
  String delete(String key);

  /**
   * Positions sequential reading at the first record whose key is not less than the given key.
   */
  String start(String key);

  /**
   * Reads the previous record of a browse (CICS READPREV); right after start, the record start positioned at.
   */
  ReadResult readPrevious();

  /**
   * The CICS RESP condition for a FILE STATUS code (DFHRESP values).
   */
  default int resp(String status) {
    return switch (status) { case "00", "02" -> 0; case "10" -> 20; case "23" -> 13; case "22" -> 14; default -> 1; };
  }

  /**
   * Outcome of a READ: the FILE STATUS code and the record ({@code null} unless the status is successful).
   */
  final class ReadResult {
    private final String status;

    private final String record;

    public ReadResult(String status, String record) {
      this.status = status;
      this.record = record;
    }

    public String status() {
      return status;
    }

    public String record() {
      return record;
    }

    public boolean successful() {
      return status.startsWith("0");
    }
  }
}
