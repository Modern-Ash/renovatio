package org.shark.renovatio.generated.cobol;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Generated from COBOL copybook CUSTOMER.cpy.
 * 
 * This record class represents the customer data structure
 * from the original COBOL copybook.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRecord {

    private String custId;
    private String custName;
    private String custAddress;
    private String custPhone;
    private String custEmail;
    private java.math.BigDecimal custBalance;
    private String custStatus;

    /**
     * Check if customer is active.
     */
    public boolean isActive() {
        return "A".equals(this.custStatus);
    }

    /**
     * Check if customer is inactive.
     */
    public boolean isInactive() {
        return "I".equals(this.custStatus);
    }

    /**
     * Check if customer is closed.
     */
    public boolean isClosed() {
        return "C".equals(this.custStatus);
    }
}
