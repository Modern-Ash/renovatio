package org.shark.renovatio.generated.cobol;

import java.lang.Override;
import org.springframework.stereotype.Service;

/**
 * Implementation of Cics001Service
 * Generated from COBOL program: Cics001
 */
@Service
public class Cics001ServiceImpl implements Cics001Service {
  @Override
  public Cics001DTO process(Cics001DTO input) {
      {
          Cics001DTO output = new Cics001DTO();
          // PERFORM INITIALIZE (paragraph not found)
          performReceiveData(input, output);
          performProcessCustomer(input, output);
          performSendResponse(input, output);
          performCleanup(input, output);
          // COBOL not translated: EXEC CICS
          // COBOL not translated: RETURN
          // COBOL not translated: INITIALIZE
          // COBOL not translated: SPACES (data item not modeled)
          // COBOL not translated: SPACES (data item not modeled)
          output.setWsResponse(0);
          // COBOL not translated: SPACES (data item not modeled)
          return output;
      }
  }

  @Override
  public boolean validate(Cics001DTO input) {
    if (input == null) { return false; };
    if (input.getWsCustomerId() == null || input.getWsCustomerId().length() > 10) { return false; };
    if (input.getWsCustomerName() == null || input.getWsCustomerName().length() > 50) { return false; };
    if (input.getWsResponse() == null) { return false; };
    if (String.valueOf(Math.abs(input.getWsResponse())).length() > 8) { return false; };
    if (input.getWsMessage() == null || input.getWsMessage().length() > 80) { return false; };
    return true;
  }
    @GeneratedFrom(paragraph = "RECEIVE-DATA", lines = "30-41")
    private void performReceiveData(Cics001DTO input, Cics001DTO out) {
        // COBOL not translated: EXEC CICS
        // COBOL not translated: RECEIVE
        // COBOL not translated: INTO (WS-CUSTOMER-ID)
        // COBOL not translated: LENGTH (10)
        // COBOL not translated: RESP (WS-RESPONSE)
        if (out.getWsResponse() != 0) {
            out.setWsMessage("Error receiving data");
            performSendError(input, out);
        }
    }
    @GeneratedFrom(paragraph = "PROCESS-CUSTOMER", lines = "42-60")
    private void performProcessCustomer(Cics001DTO input, Cics001DTO out) {
        // COBOL not translated: CUST-ID (data item not modeled)
        // COBOL not translated: EXEC CICS
        // READ UNKNOWN
        // COBOL not translated: FILE('CUSTOMERS')
        // COBOL not translated: INTO (CUSTOMER-RECORD)
        // COBOL not translated: LENGTH (LENGTH OF CUSTOMER-RECORD)
        // COBOL not translated: KEY (WS-CUSTOMER-ID)
        // COBOL not translated: RESP (WS-RESPONSE)
        if (out.getWsResponse() == 0) {
            // COBOL not translated: CUST-NAME (data item not modeled)
            // COBOL not translated: STRING 'Customer: ' DELIMITED BY SIZE
            // COBOL not translated: WS-CUSTOMER-NAME DELIMITED BY SIZE
            // COBOL not translated: INTO WS-MESSAGE
        } else {
            out.setWsMessage("Customer not found");
        }
    }
    @GeneratedFrom(paragraph = "SEND-RESPONSE", lines = "61-68")
    private void performSendResponse(Cics001DTO input, Cics001DTO out) {
        // COBOL not translated: EXEC CICS
        // COBOL not translated: SEND
        // COBOL not translated: FROM (WS-MESSAGE)
        // COBOL not translated: LENGTH (LENGTH OF WS-MESSAGE)
        // COBOL not translated: RESP (WS-RESPONSE)
    }
    @GeneratedFrom(paragraph = "CLEANUP", lines = "80-84")
    private void performCleanup(Cics001DTO input, Cics001DTO out) {
        // COBOL not translated: SPACES (data item not modeled)
        // COBOL not translated: SPACES (data item not modeled)
        out.setWsResponse(0);
        // COBOL not translated: SPACES (data item not modeled)
    }
    @GeneratedFrom(paragraph = "SEND-ERROR", lines = "69-79")
    private void performSendError(Cics001DTO input, Cics001DTO out) {
        // COBOL not translated: EXEC CICS
        // COBOL not translated: SEND
        // COBOL not translated: FROM (WS-MESSAGE)
        // COBOL not translated: LENGTH (LENGTH OF WS-MESSAGE)
        // COBOL not translated: RESP (WS-RESPONSE)
        // COBOL not translated: EXEC CICS
        // COBOL not translated: RETURN
    }
}@interface GeneratedFrom {
    String paragraph();
    String lines();
}
