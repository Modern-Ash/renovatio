package org.modernash.renovatio.generated.cobol;

import java.lang.Integer;
import java.lang.Number;
import java.lang.String;
import java.math.BigDecimal;

/**
 * Data Transfer Object generated from COBOL program: Db2001
 */
public class Db2001DTO {
  private Integer wsEmpId;

  private String wsEmpName;

  private String wsEmpDept;

  private BigDecimal wsEmpSalary;

  private Integer wsResponse;

  private String wsMessage;

  private Integer empId;

  private String empName;

  private String empDept;

  private BigDecimal empSalary;

  private String empHireDate;

  private String empStatus;

  public Db2001DTO() {
    this.wsEmpId = 0;
    this.wsEmpName = "                                                  ";
    this.wsEmpDept = "                    ";
    this.wsEmpSalary = new java.math.BigDecimal("0");
    this.wsResponse = 0;
    this.wsMessage = "                                                                                ";
    this.empId = 0;
    this.empName = "                                                  ";
    this.empDept = "                    ";
    this.empSalary = new java.math.BigDecimal("0");
    this.empHireDate = "          ";
    this.empStatus = " ";
  }

  public Integer getWsEmpId() {
    return wsEmpId;
  }

  public void setWsEmpId(Integer wsEmpId) {
    this.wsEmpId = wsEmpId;
  }

  public String getWsEmpName() {
    return wsEmpName;
  }

  public void setWsEmpName(String wsEmpName) {
    this.wsEmpName = wsEmpName == null ? "                                                  " : String.format("%-50.50s", wsEmpName);
  }

  public String getWsEmpDept() {
    return wsEmpDept;
  }

  public void setWsEmpDept(String wsEmpDept) {
    this.wsEmpDept = wsEmpDept == null ? "                    " : String.format("%-20.20s", wsEmpDept);
  }

  public BigDecimal getWsEmpSalary() {
    return wsEmpSalary;
  }

  public void setWsEmpSalary(BigDecimal wsEmpSalary) {
    this.wsEmpSalary = wsEmpSalary;
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

  public Integer getEmpId() {
    return empId;
  }

  public void setEmpId(Integer empId) {
    this.empId = empId;
  }

  public String getEmpName() {
    return empName;
  }

  public void setEmpName(String empName) {
    this.empName = empName == null ? "                                                  " : String.format("%-50.50s", empName);
  }

  public String getEmpDept() {
    return empDept;
  }

  public void setEmpDept(String empDept) {
    this.empDept = empDept == null ? "                    " : String.format("%-20.20s", empDept);
  }

  public BigDecimal getEmpSalary() {
    return empSalary;
  }

  public void setEmpSalary(BigDecimal empSalary) {
    this.empSalary = empSalary;
  }

  public String getEmpHireDate() {
    return empHireDate;
  }

  public void setEmpHireDate(String empHireDate) {
    this.empHireDate = empHireDate == null ? "          " : String.format("%-10.10s", empHireDate);
  }

  public String getEmpStatus() {
    return empStatus;
  }

  public void setEmpStatus(String empStatus) {
    this.empStatus = empStatus == null ? " " : String.format("%-1.1s", empStatus);
  }

  /**
   * COBOL group EMPLOYEE-RECORD as its 89-character DISPLAY record.
   */
  public String getEmployeeRecord() {
    return fitBinary(empId, 2, 0, true) + fitText(empName, 50) + fitText(empDept, 20) + fitPacked(empSalary, 6, 2, true) + fitText(empHireDate, 10) + fitText(empStatus, 1);
  }

  /**
   * Splits a 89-character DISPLAY record into the fields of COBOL group EMPLOYEE-RECORD.
   */
  public void setEmployeeRecord(String record) {
    String r = fitText(record, 89);
    this.empId = parseBinary(r.substring(0, 2), 0, true).intValue();
    this.empName = r.substring(2, 52);
    this.empDept = r.substring(52, 72);
    this.empSalary = parsePacked(r.substring(72, 78), 2, true);
    this.empHireDate = r.substring(78, 88);
    this.empStatus = r.substring(88, 89);
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
}
