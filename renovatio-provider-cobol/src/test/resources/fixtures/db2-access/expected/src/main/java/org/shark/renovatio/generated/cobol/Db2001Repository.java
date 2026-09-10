package org.shark.renovatio.generated.cobol;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Generated from COBOL program DB2001.
 * 
 * This repository implements the DB2 data access logic
 * from the original COBOL program. It uses Spring JDBC
 * for database operations.
 */
@Slf4j
@Repository
public class Db2001Repository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private int empId;
    private String empName;
    private String empDept;
    private BigDecimal empSalary;
    private int response;
    private String message;

    /**
     * Main entry point equivalent to MAIN-LOGIC paragraph.
     */
    public EmpRecEntity processEmployee(int empId) {
        this.empId = empId;
        initialize();
        EmpRecEntity employee = fetchEmployee();
        if (employee != null) {
            updateSalary(employee);
        }
        displayResult();
        cleanup();
        return employee;
    }

    /**
     * Initialize working storage variables.
     * Equivalent to INITIALIZE paragraph.
     */
    public void initialize() {
        this.empId = 0;
        this.empName = "";
        this.empDept = "";
        this.empSalary = BigDecimal.ZERO;
        this.response = 0;
        this.message = "";
        log.debug("Initialized DB2 working storage");
    }

    /**
     * Fetch employee from database.
     * Equivalent to FETCH-EMPLOYEE paragraph.
     */
    public EmpRecEntity fetchEmployee() {
        String sql = "SELECT EMP_ID, EMP_NAME, EMP_DEPT, EMP_SALARY, EMP_HIRE_DATE, EMP_STATUS " +
                     "FROM EMPLOYEES WHERE EMP_ID = ?";
        
        try {
            List<EmpRecEntity> results = jdbcTemplate.query(sql, new Object[]{this.empId}, 
                new RowMapper<EmpRecEntity>() {
                    @Override
                    public EmpRecEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
                        EmpRecEntity entity = new EmpRecEntity();
                        entity.setEmpId(rs.getInt("EMP_ID"));
                        entity.setEmpName(rs.getString("EMP_NAME"));
                        entity.setEmpDept(rs.getString("EMP_DEPT"));
                        entity.setEmpSalary(rs.getBigDecimal("EMP_SALARY"));
                        entity.setEmpHireDate(rs.getString("EMP_HIRE_DATE"));
                        entity.setEmpStatus(rs.getString("EMP_STATUS"));
                        return entity;
                    }
                });
            
            if (!results.isEmpty()) {
                EmpRecEntity employee = results.get(0);
                this.empName = employee.getEmpName();
                this.empDept = employee.getEmpDept();
                this.empSalary = employee.getEmpSalary();
                this.message = "Employee: " + this.empName;
                log.debug("Fetched employee: {}", this.empName);
                return employee;
            } else {
                this.message = "Employee not found";
                log.warn("Employee not found: {}", this.empId);
                return null;
            }
        } catch (Exception e) {
            this.message = "Error fetching employee: " + e.getMessage();
            log.error("Error fetching employee", e);
            return null;
        }
    }

    /**
     * Update employee salary.
     * Equivalent to UPDATE-SALARY paragraph.
     */
    public void updateSalary(EmpRecEntity employee) {
        BigDecimal newSalary = employee.getEmpSalary().multiply(new BigDecimal("1.05"));
        String sql = "UPDATE EMPLOYEES SET EMP_SALARY = ? WHERE EMP_ID = ?";
        
        try {
            int rowsAffected = jdbcTemplate.update(sql, newSalary, this.empId);
            if (rowsAffected > 0) {
                this.message = "Salary updated successfully";
                log.debug("Updated salary for employee {}: {} -> {}", 
                    this.empId, employee.getEmpSalary(), newSalary);
            } else {
                this.message = "Error updating salary";
                log.error("Failed to update salary for employee {}", this.empId);
            }
        } catch (Exception e) {
            this.message = "Error updating salary: " + e.getMessage();
            log.error("Error updating salary", e);
        }
    }

    /**
     * Display result message.
     * Equivalent to DISPLAY-RESULT paragraph.
     */
    public void displayResult() {
        log.info("Result: {}", this.message);
        System.out.println(this.message);
    }

    /**
     * Cleanup working storage.
     * Equivalent to CLEANUP paragraph.
     */
    public void cleanup() {
        this.empId = 0;
        this.empName = "";
        this.empDept = "";
        this.empSalary = BigDecimal.ZERO;
        this.response = 0;
        this.message = "";
        log.debug("Cleaned up DB2 working storage");
    }

    /**
     * Find employee by ID.
     */
    public Optional<EmpRecEntity> findById(int empId) {
        String sql = "SELECT * FROM EMPLOYEES WHERE EMP_ID = ?";
        List<EmpRecEntity> results = jdbcTemplate.query(sql, new Object[]{empId}, 
            (rs, rowNum) -> mapRow(rs));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Save employee.
     */
    public EmpRecEntity save(EmpRecEntity entity) {
        String sql = "INSERT INTO EMPLOYEES (EMP_ID, EMP_NAME, EMP_DEPT, EMP_SALARY, EMP_HIRE_DATE, EMP_STATUS) " +
                     "VALUES (?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "EMP_NAME = VALUES(EMP_NAME), EMP_DEPT = VALUES(EMP_DEPT), " +
                     "EMP_SALARY = VALUES(EMP_SALARY), EMP_STATUS = VALUES(EMP_STATUS)";
        
        jdbcTemplate.update(sql, 
            entity.getEmpId(), entity.getEmpName(), entity.getEmpDept(),
            entity.getEmpSalary(), entity.getEmpHireDate(), entity.getEmpStatus());
        
        return entity;
    }

    /**
     * Delete employee by ID.
     */
    public void deleteById(int empId) {
        String sql = "DELETE FROM EMPLOYEES WHERE EMP_ID = ?";
        jdbcTemplate.update(sql, empId);
    }

    private EmpRecEntity mapRow(ResultSet rs) throws SQLException {
        EmpRecEntity entity = new EmpRecEntity();
        entity.setEmpId(rs.getInt("EMP_ID"));
        entity.setEmpName(rs.getString("EMP_NAME"));
        entity.setEmpDept(rs.getString("EMP_DEPT"));
        entity.setEmpSalary(rs.getBigDecimal("EMP_SALARY"));
        entity.setEmpHireDate(rs.getString("EMP_HIRE_DATE"));
        entity.setEmpStatus(rs.getString("EMP_STATUS"));
        return entity;
    }

    /**
     * Get current employee ID.
     */
    public int getEmpId() {
        return empId;
    }

    /**
     * Get current employee name.
     */
    public String getEmpName() {
        return empName;
    }

    /**
     * Get current employee department.
     */
    public String getEmpDept() {
        return empDept;
    }

    /**
     * Get current employee salary.
     */
    public BigDecimal getEmpSalary() {
        return empSalary;
    }

    /**
     * Get current response code.
     */
    public int getResponse() {
        return response;
    }

    /**
     * Get current message.
     */
    public String getMessage() {
        return message;
    }
}
