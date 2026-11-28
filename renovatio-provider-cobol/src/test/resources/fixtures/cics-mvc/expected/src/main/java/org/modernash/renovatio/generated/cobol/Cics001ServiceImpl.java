package org.modernash.renovatio.generated.cobol;

import java.lang.Override;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of Cics001Service
 * Generated from COBOL program: Cics001
 */
@Service
public class Cics001ServiceImpl implements Cics001Service {
  private final Cics001CustomersPort customers;

  /**
   * Standalone wiring with in-memory file adapters and default program adapters.
   */
  public Cics001ServiceImpl() {
    this(new Cics001CustomersInMemoryAdapter());
  }

  @Autowired
  public Cics001ServiceImpl(Cics001CustomersPort customers) {
    this.customers = customers;
  }

  @Override
  public Cics001DTO process(Cics001DTO input) {
      {
          Cics001DTO output = new Cics001DTO();
          try {
              // PERFORM INITIALIZE (paragraph not found)
              performReceiveData(input, output);
              performProcessCustomer(input, output);
              performSendResponse(input, output);
              performCleanup(input, output);
              output.getScreenResponse().end();
              output.endRun();
          } catch (Cics001DTO.ProgramExit ended) {
              // the run ended in a performed paragraph (GOBACK, STOP RUN, XCTL or RETURN)
          }
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
    @GeneratedFrom(paragraph = "RECEIVE-DATA", lines = "40-51")
    private void performReceiveData(Cics001DTO input, Cics001DTO out) {
        // COBOL not translated: EXEC CICS RECEIVE INTO (WS-CUSTOMER-ID) LENGTH (10) RESP (WS-RESPONSE) END-EXEC
        if (out.getWsResponse() != 0) {
            out.setWsMessage("Error receiving data");
            performSendError(input, out);
        }
    }
    @GeneratedFrom(paragraph = "PROCESS-CUSTOMER", lines = "52-70")
    private void performProcessCustomer(Cics001DTO input, Cics001DTO out) {
        out.setCustId(out.getWsCustomerId());
        // COBOL not translated: EXEC CICS READ FILE('CUSTOMERS') INTO (CUSTOMER-RECORD) LENGTH (LENGTH OF CUSTOMER-RECORD) KEY (WS-CUSTOMER-ID) RESP (WS
        if (out.getWsResponse() == 0) {
            out.setWsCustomerName(out.getCustName());
            String stringValue2 = "Customer: " + out.getWsCustomerName();
            String stringTarget2 = out.getWsMessage();
            int stringStart2 = 0;
            if (stringStart2 >= 0 && stringStart2 < stringTarget2.length()) {
                int stringCount2 = Math.min(stringValue2.length(), stringTarget2.length() - stringStart2);
                out.setWsMessage(stringTarget2.substring(0, stringStart2) + stringValue2.substring(0, stringCount2) + stringTarget2.substring(stringStart2 + stringCount2));
            }
        } else {
            out.setWsMessage("Customer not found");
        }
    }
    @GeneratedFrom(paragraph = "SEND-RESPONSE", lines = "71-78")
    private void performSendResponse(Cics001DTO input, Cics001DTO out) {
        out.getScreenResponse().sendText(out.getWsMessage());
        out.setWsResponse(0);
    }
    @GeneratedFrom(paragraph = "CLEANUP", lines = "90-94")
    private void performCleanup(Cics001DTO input, Cics001DTO out) {
        out.setWsCustomerId("          ");
        out.setWsCustomerName("                                                  ");
        out.setWsResponse(0);
        out.setWsMessage("                                                                                ");
    }
    @GeneratedFrom(paragraph = "SEND-ERROR", lines = "79-89")
    private void performSendError(Cics001DTO input, Cics001DTO out) {
        out.getScreenResponse().sendText(out.getWsMessage());
        out.setWsResponse(0);
        out.getScreenResponse().end();
        out.endRun();
    }
}@interface GeneratedFrom {
    String paragraph();
    String lines();
}
