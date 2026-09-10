package org.shark.renovatio.generated.cobol;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Generated from COBOL copybook EMP-REC.cpy.
 * 
 * This entity class represents the employee data structure
 * from the original COBOL copybook, mapped to JPA entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "EMPLOYEES")
public class EmpRecEntity {

    @Id
    @Column(name = "EMP_ID")
    private int empId;

    @Column(name = "EMP_NAME")
    private String empName;

    @Column(name = "EMP_DEPT")
    private String empDept;

    @Column(name = "EMP_SALARY")
    private java.math.BigDecimal empSalary;

    @Column(name = "EMP_HIRE_DATE")
    private String empHireDate;

    @Column(name = "EMP_STATUS")
    private String empStatus;

    /**
     * Check if employee is active.
     */
    public boolean isActive() {
        return "A".equals(this.empStatus);
    }

    /**
     * Check if employee is inactive.
     */
    public boolean isInactive() {
        return "I".equals(this.empStatus);
    }

    /**
     * Check if employee is terminated.
     */
    public boolean isTerminated() {
        return "T".equals(this.empStatus);
    }
}
