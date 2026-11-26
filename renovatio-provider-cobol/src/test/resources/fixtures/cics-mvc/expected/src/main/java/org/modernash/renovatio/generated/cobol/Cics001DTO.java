package org.modernash.renovatio.generated.cobol;

import java.lang.Integer;
import java.lang.String;
import java.math.BigDecimal;

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
    this.wsCustomerId = wsCustomerId;
  }

  public String getWsCustomerName() {
    return wsCustomerName;
  }

  public void setWsCustomerName(String wsCustomerName) {
    this.wsCustomerName = wsCustomerName;
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
    this.wsMessage = wsMessage;
  }

  public String getCustId() {
    return custId;
  }

  public void setCustId(String custId) {
    this.custId = custId;
  }

  public String getCustName() {
    return custName;
  }

  public void setCustName(String custName) {
    this.custName = custName;
  }

  public String getCustAddress() {
    return custAddress;
  }

  public void setCustAddress(String custAddress) {
    this.custAddress = custAddress;
  }

  public String getCustPhone() {
    return custPhone;
  }

  public void setCustPhone(String custPhone) {
    this.custPhone = custPhone;
  }

  public String getCustEmail() {
    return custEmail;
  }

  public void setCustEmail(String custEmail) {
    this.custEmail = custEmail;
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
    this.custStatus = custStatus;
  }
}
