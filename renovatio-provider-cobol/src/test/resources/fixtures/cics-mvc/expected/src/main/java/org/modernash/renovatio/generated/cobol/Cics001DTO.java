package org.modernash.renovatio.generated.cobol;

import java.lang.IllegalStateException;
import java.lang.Integer;
import java.lang.Number;
import java.lang.RuntimeException;
import java.lang.String;
import java.lang.System;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Data Transfer Object generated from COBOL program: Cics001
 */
public class Cics001DTO {
  private String wsCustomerId;

  private String wsCustomerName;

  private Integer wsResponse;

  private String wsMessage;

  private String custId;

  private String custName;

  private String custAddress;

  private String custPhone;

  private String custEmail;

  private BigDecimal custBalance;

  private String custStatus;

  private ScreenRequest screenRequest = ScreenRequest.firstEntry();

  private final ScreenResponse screenResponse = new ScreenResponse();

  public Cics001DTO() {
    this.wsCustomerId = "          ";
    this.wsCustomerName = "                                                  ";
    this.wsResponse = 0;
    this.wsMessage = "                                                                                ";
    this.custId = "          ";
    this.custName = "                                                  ";
    this.custAddress = "                                                                                                    ";
    this.custPhone = "               ";
    this.custEmail = "                                                  ";
    this.custBalance = new java.math.BigDecimal("0");
    this.custStatus = " ";
  }

  public String getWsCustomerId() {
    return wsCustomerId;
  }

  public void setWsCustomerId(String wsCustomerId) {
    this.wsCustomerId = wsCustomerId == null ? "          " : String.format("%-10.10s", wsCustomerId);
  }

  public String getWsCustomerName() {
    return wsCustomerName;
  }

  public void setWsCustomerName(String wsCustomerName) {
    this.wsCustomerName = wsCustomerName == null ? "                                                  " : String.format("%-50.50s", wsCustomerName);
  }

  public Integer getWsResponse() {
    return wsResponse;
  }

  public void setWsResponse(Integer wsResponse) {
    this.wsResponse = wsResponse;
  }

  public String getWsMessage() {
    return wsMessage;
  }

  public void setWsMessage(String wsMessage) {
    this.wsMessage = wsMessage == null ? "                                                                                " : String.format("%-80.80s", wsMessage);
  }

  public String getCustId() {
    return custId;
  }

  public void setCustId(String custId) {
    this.custId = custId == null ? "          " : String.format("%-10.10s", custId);
  }

  public String getCustName() {
    return custName;
  }

  public void setCustName(String custName) {
    this.custName = custName == null ? "                                                  " : String.format("%-50.50s", custName);
  }

  public String getCustAddress() {
    return custAddress;
  }

  public void setCustAddress(String custAddress) {
    this.custAddress = custAddress == null ? "                                                                                                    " : String.format("%-100.100s", custAddress);
  }

  public String getCustPhone() {
    return custPhone;
  }

  public void setCustPhone(String custPhone) {
    this.custPhone = custPhone == null ? "               " : String.format("%-15.15s", custPhone);
  }

  public String getCustEmail() {
    return custEmail;
  }

  public void setCustEmail(String custEmail) {
    this.custEmail = custEmail == null ? "                                                  " : String.format("%-50.50s", custEmail);
  }

  public BigDecimal getCustBalance() {
    return custBalance;
  }

  public void setCustBalance(BigDecimal custBalance) {
    this.custBalance = custBalance;
  }

  public String getCustStatus() {
    return custStatus;
  }

  public void setCustStatus(String custStatus) {
    this.custStatus = custStatus == null ? " " : String.format("%-1.1s", custStatus);
  }

  /**
   * COBOL group CUSTOMER-RECORD as its 232-character DISPLAY record.
   */
  public String getCustomerRecord() {
    return fitText(custId, 10) + fitText(custName, 50) + fitText(custAddress, 100) + fitText(custPhone, 15) + fitText(custEmail, 50) + fitPacked(custBalance, 6, 2, true) + fitText(custStatus, 1);
  }

  /**
   * Splits a 232-character DISPLAY record into the fields of COBOL group CUSTOMER-RECORD.
   */
  public void setCustomerRecord(String record) {
    String r = fitText(record, 232);
    this.custId = r.substring(0, 10);
    this.custName = r.substring(10, 60);
    this.custAddress = r.substring(60, 160);
    this.custPhone = r.substring(160, 175);
    this.custEmail = r.substring(175, 225);
    this.custBalance = parsePacked(r.substring(225, 231), 2, true);
    this.custStatus = r.substring(231, 232);
  }

  private static String fitText(String value, int length) {
    String text = value == null ? "" : value;
    return text.length() >= length ? text.substring(0, length) : text + " ".repeat(length - text.length());
  }

  private static String fitNumber(Number value, int length, int scale, boolean signed) {
    java.math.BigDecimal number = value == null ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(value.toString());
    String digits = number.setScale(scale, java.math.RoundingMode.DOWN).unscaledValue().abs().toString();
    digits = digits.length() > length ? digits.substring(digits.length() - length) : "0".repeat(length - digits.length()) + digits;
    if (!signed) {
      return digits;
    }
    int last = digits.charAt(length - 1) - '0';
    char zone = number.signum() < 0 ? "}JKLMNOPQR".charAt(last) : "{ABCDEFGHI".charAt(last);
    return digits.substring(0, length - 1) + zone;
  }

  private static BigDecimal parseNumber(String text, int scale, boolean signed) {
    boolean negative = false;
    StringBuilder digits = new StringBuilder();
    for (char c : text.toCharArray()) {
      if (c >= '0' && c <= '9') {
        digits.append(c);
      } else if (signed && "{ABCDEFGHI".indexOf(c) >= 0) {
        digits.append((char) ('0' + "{ABCDEFGHI".indexOf(c)));
      } else if (signed && "}JKLMNOPQR".indexOf(c) >= 0) {
        digits.append((char) ('0' + "}JKLMNOPQR".indexOf(c)));
        negative = true;
      } else if (signed && c >= 'p' && c <= 'y') {
        digits.append((char) ('0' + (c - 'p')));
        negative = true;
      } else {
        // Spaces or other non-digits in a numeric field read as zero.
        digits.append('0');
      }
    }
    java.math.BigDecimal number = new java.math.BigDecimal(new java.math.BigInteger(digits.toString()), scale);
    return negative ? number.negate() : number;
  }

  private static String fitBinary(Number value, int length, int scale, boolean signed) {
    java.math.BigDecimal number = value == null ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(value.toString());
    long unscaled = number.setScale(scale, java.math.RoundingMode.DOWN).unscaledValue().longValue();
    if (!signed) {
      unscaled = Math.abs(unscaled);
    }
    char[] bytes = new char[length];
    for (int i = length - 1; i >= 0; i--) {
      bytes[i] = (char) (unscaled & 0xFF);
      unscaled >>= 8;
    }
    return new String(bytes);
  }

  private static BigDecimal parseBinary(String bytes, int scale, boolean signed) {
    long unscaled = signed && bytes.length() > 0 && (bytes.charAt(0) & 0x80) != 0 ? -1L : 0L;
    for (int i = 0; i < bytes.length(); i++) {
      unscaled = (unscaled << 8) | (bytes.charAt(i) & 0xFF);
    }
    return java.math.BigDecimal.valueOf(unscaled, scale);
  }

  private static String fitPacked(Number value, int length, int scale, boolean signed) {
    java.math.BigDecimal number = value == null ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(value.toString());
    String digits = number.setScale(scale, java.math.RoundingMode.DOWN).unscaledValue().abs().toString();
    int width = length * 2 - 1;
    digits = digits.length() > width ? digits.substring(digits.length() - width) : "0".repeat(width - digits.length()) + digits;
    int sign = !signed ? 0xF : number.signum() < 0 ? 0xD : 0xC;
    char[] bytes = new char[length];
    for (int i = 0; i < length; i++) {
      int high = digits.charAt(2 * i) - '0';
      int low = i == length - 1 ? sign : digits.charAt(2 * i + 1) - '0';
      bytes[i] = (char) ((high << 4) | low);
    }
    return new String(bytes);
  }

  private static BigDecimal parsePacked(String bytes, int scale, boolean signed) {
    StringBuilder digits = new StringBuilder();
    int sign = 0xF;
    for (int i = 0; i < bytes.length(); i++) {
      int b = bytes.charAt(i) & 0xFF;
      digits.append(Math.min(b >> 4, 9));
      if (i == bytes.length() - 1) {
        sign = b & 0x0F;
      } else {
        digits.append(Math.min(b & 0x0F, 9));
      }
    }
    java.math.BigDecimal number = new java.math.BigDecimal(new java.math.BigInteger(digits.length() == 0 ? "0" : digits.toString()), scale);
    return signed && (sign == 0xD || sign == 0xB) ? number.negate() : number;
  }

  /**
   * Ends the program run; {@code process} catches it and returns this DTO.
   */
  public void endRun() {
    throw new ProgramExit();
  }

  public ScreenRequest getScreenRequest() {
    return screenRequest;
  }

  public void setScreenRequest(ScreenRequest screenRequest) {
    this.screenRequest = screenRequest;
  }

  public ScreenResponse getScreenResponse() {
    return screenResponse;
  }

  /**
   * CICS ABEND: the task fails with the abend code; nothing is returned.
   */
  public void abend(String code) {
    throw new IllegalStateException("CICS ABEND " + code);
  }

  /**
   * Ends the program run (GOBACK, STOP RUN, CICS XCTL or RETURN) from any paragraph.
   */
  public static final class ProgramExit extends RuntimeException {
    ProgramExit() {
      super(null, null, false, false);
    }
  }

  /**
   * Input of one task: the attention key (ENTER, CLEAR, PF1..PF24, PA1..PA3), the COMMAREA
   * the previous task returned (empty on first entry) and the received map data by map name.
   */
  public static final class ScreenRequest {
    private final String aid;

    private final String commarea;

    private final Map<String, String> maps;

    public ScreenRequest(String aid, String commarea, Map<String, String> maps) {
      this.aid = aid;
      this.commarea = commarea == null ? "" : commarea;
      this.maps = maps == null ? Map.of() : Map.copyOf(maps);
    }

    /**
     * A transaction started from scratch: no COMMAREA (EIBCALEN = 0).
     */
    public static ScreenRequest firstEntry() {
      return new ScreenRequest("ENTER", "", Map.of());
    }

    public String aid() {
      return aid;
    }

    public String commarea() {
      return commarea;
    }

    public Integer commareaLength() {
      return commarea.length();
    }

    /**
     * CICS ASSIGN APPLID: the region's application id (environment CICS_APPLID).
     */
    public String applid() {
      return System.getenv().getOrDefault("CICS_APPLID", "");
    }

    /**
     * CICS ASSIGN SYSID: the system id (environment CICS_SYSID).
     */
    public String sysid() {
      return System.getenv().getOrDefault("CICS_SYSID", "");
    }

    public String map(String name) {
      return maps.getOrDefault(name, "");
    }
  }

  /**
   * Outcome of one task: the screen sent (map and data, or plain text) and where the
   * conversation goes next (transfer to a program, or resume a transaction with a COMMAREA).
   */
  public static final class ScreenResponse {
    private String map;

    private String mapset;

    private String screen;

    private String text;

    private String nextProgram;

    private String transid;

    private String commarea;

    private boolean ended;

    public String map() {
      return map;
    }

    public String mapset() {
      return mapset;
    }

    public String screen() {
      return screen;
    }

    public String text() {
      return text;
    }

    public String nextProgram() {
      return nextProgram;
    }

    public String transid() {
      return transid;
    }

    public String commarea() {
      return commarea;
    }

    public boolean ended() {
      return ended;
    }

    /**
     * SEND MAP: show {@code screen} (the symbolic output map) with map/mapset.
     */
    public void sendMap(String map, String mapset, String screen) {
      this.map = map;
      this.mapset = mapset;
      this.screen = screen;
      this.text = null;
    }

    /**
     * SEND TEXT: show plain text instead of a map.
     */
    public void sendText(String text) {
      this.text = text;
      this.map = null;
      this.screen = null;
    }

    /**
     * XCTL: hand the conversation to {@code program} with {@code commarea}.
     */
    public void transfer(String program, String commarea) {
      this.nextProgram = program;
      this.commarea = commarea;
      this.ended = true;
    }

    /**
     * RETURN TRANSID: resume with {@code transid} and {@code commarea} on the next input.
     */
    public void returnTo(String transid, String commarea) {
      this.transid = transid;
      this.commarea = commarea;
      this.ended = true;
    }

    /**
     * RETURN without TRANSID: the conversation is over.
     */
    public void end() {
      this.ended = true;
    }
  }
}
