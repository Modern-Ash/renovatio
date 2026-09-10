package org.shark.renovatio.generated.cobol;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Generated from COBOL program CICS001.
 * 
 * This controller implements the CICS transaction processing logic
 * from the original COBOL program. It handles customer data retrieval
 * and response generation.
 */
@Slf4j
@RestController
@RequestMapping("/api/cics")
public class Cics001Controller {

    private String customerId;
    private String customerName;
    private int response;
    private String message;

    /**
     * Main entry point equivalent to MAIN-LOGIC paragraph.
     */
    @GetMapping("/customer/{customerId}")
    public String handleRequest(@PathVariable String customerId, 
                               HttpServletResponse httpResponse) throws IOException {
        this.customerId = customerId;
        initialize();
        receiveData();
        processCustomer();
        sendResponse(httpResponse);
        cleanup();
        return this.message;
    }

    /**
     * Initialize working storage variables.
     * Equivalent to INITIALIZE paragraph.
     */
    public void initialize() {
        this.customerId = "";
        this.customerName = "";
        this.response = 0;
        this.message = "";
        log.debug("Initialized CICS working storage");
    }

    /**
     * Receive data from CICS terminal.
     * Equivalent to RECEIVE-DATA paragraph.
     */
    public void receiveData() {
        if (this.customerId == null || this.customerId.isEmpty()) {
            this.message = "Error receiving data";
            log.error("Failed to receive customer ID");
            return;
        }
        log.debug("Received customer ID: {}", this.customerId);
    }

    /**
     * Process customer data from file.
     * Equivalent to PROCESS-CUSTOMER paragraph.
     */
    public void processCustomer() {
        // In real implementation, this would read from file system
        // For now, we simulate the customer lookup
        if (this.customerId != null && !this.customerId.isEmpty()) {
            this.customerName = "Customer " + this.customerId;
            this.message = "Customer: " + this.customerName;
            log.debug("Processed customer: {}", this.customerName);
        } else {
            this.message = "Customer not found";
            log.warn("Customer not found: {}", this.customerId);
        }
    }

    /**
     * Send response back to CICS terminal.
     * Equivalent to SEND-RESPONSE paragraph.
     */
    public void sendResponse(HttpServletResponse httpResponse) throws IOException {
        httpResponse.getWriter().write(this.message);
        log.debug("Sent response: {}", this.message);
    }

    /**
     * Send error response.
     * Equivalent to SEND-ERROR paragraph.
     */
    public void sendError(HttpServletResponse httpResponse) throws IOException {
        httpResponse.getWriter().write(this.message);
        log.error("Sent error response: {}", this.message);
    }

    /**
     * Cleanup working storage.
     * Equivalent to CLEANUP paragraph.
     */
    public void cleanup() {
        this.customerId = "";
        this.customerName = "";
        this.response = 0;
        this.message = "";
        log.debug("Cleaned up CICS working storage");
    }

    /**
     * Get current customer ID.
     */
    public String getCustomerId() {
        return customerId;
    }

    /**
     * Get current customer name.
     */
    public String getCustomerName() {
        return customerName;
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
