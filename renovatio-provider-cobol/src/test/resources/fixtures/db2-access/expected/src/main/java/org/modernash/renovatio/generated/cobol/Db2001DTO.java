package org.modernash.renovatio.generated.cobol;

import java.lang.Integer;
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
    this.wsEmpSalary = new java.math.BigDecimal("0");
    this.wsResponse = 0;
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
    this.wsEmpName = wsEmpName;
  }

  public String getWsEmpDept() {
    return wsEmpDept;
  }

  public void setWsEmpDept(String wsEmpDept) {
    this.wsEmpDept = wsEmpDept;
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
    this.wsMessage = wsMessage;
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
    this.empName = empName;
  }

  public String getEmpDept() {
    return empDept;
  }

  public void setEmpDept(String empDept) {
    this.empDept = empDept;
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
    this.empHireDate = empHireDate;
  }

  public String getEmpStatus() {
    return empStatus;
  }

  public void setEmpStatus(String empStatus) {
    this.empStatus = empStatus;
  }
}
