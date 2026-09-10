package org.shark.renovatio.generated.cobol;

import java.lang.Override;
import org.springframework.stereotype.Service;

/**
 * Implementation of Batch001Service
 * Generated from COBOL program: Batch001
 */
@Service
public class Batch001ServiceImpl implements Batch001Service {
  @Override
  public Batch001DTO process(Batch001DTO input) {
      {
          Batch001DTO output = new Batch001DTO();
          // PERFORM INITIALIZE (paragraph not found)
          performProcessData(input, output);
          performDisplayResult(input, output);
          performCleanup(input, output);
          return output;
      }
  }

  @Override
  public boolean validate(Batch001DTO input) {
    if (input == null) { return false; };
    if (input.getWsCounter() == null) { return false; };
    if (String.valueOf(Math.abs(input.getWsCounter())).length() > 4) { return false; };
    if (input.getWsTotal() == null) { return false; };
    if (String.valueOf(Math.abs(input.getWsTotal())).length() > 8) { return false; };
    if (input.getWsResult() == null) { return false; };
    if (String.valueOf(Math.abs(input.getWsResult())).length() > 8) { return false; };
    if (input.getWsFlag() == null || input.getWsFlag().length() > 2) { return false; };
    if (input.getWsOutput() == null || input.getWsOutput().length() > 80) { return false; };
    return true;
  }
    @GeneratedFrom(paragraph = "PROCESS-DATA", lines = "26-35")
    private void performProcessData(Batch001DTO input, Batch001DTO out) {
        for (int wsCounter = 1; true; wsCounter += 1) {
            // COBOL not translated: UNTIL WS-COUNTER > 10
            // COBOL not translated: WS, COUNTER (data item not modeled)
            // COBOL not translated: WS, TOTAL, RESULT (data item not modeled)
            if (out.getWsTotal() > 50) {
                out.setWsFlag("Y");
            }
        }
    }
    @GeneratedFrom(paragraph = "DISPLAY-RESULT", lines = "36-43")
    private void performDisplayResult(Batch001DTO input, Batch001DTO out) {
        if (out.getWsFlag() == "Y") {
            out.setWsOutput("Total exceeded 50");
        } else {
            out.setWsOutput("Total within limit");
        }
        System.out.println(input.getWsOutput());
    }
    @GeneratedFrom(paragraph = "CLEANUP", lines = "44-47")
    private void performCleanup(Batch001DTO input, Batch001DTO out) {
        out.setWsCounter(0);
        out.setWsTotal(0);
        out.setWsResult(0);
    }
}@interface GeneratedFrom {
    String paragraph();
    String lines();
}
