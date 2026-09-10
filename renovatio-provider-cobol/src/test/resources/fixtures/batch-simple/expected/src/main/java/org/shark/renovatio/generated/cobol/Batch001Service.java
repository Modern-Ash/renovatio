package org.shark.renovatio.generated.cobol;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Generated from COBOL program BATCH001.
 * 
 * This service implements the batch processing logic from the original COBOL program.
 * It processes data through a loop, performs calculations, and displays results.
 */
@Slf4j
@Service
public class Batch001Service {

    private int counter;
    private long total;
    private long result;
    private String flag;
    private String output;

    /**
     * Main entry point equivalent to MAIN-LOGIC paragraph.
     */
    public void process() {
        initialize();
        processData();
        displayResult();
        cleanup();
    }

    /**
     * Initialize working storage variables.
     * Equivalent to INITIALIZE paragraph.
     */
    public void initialize() {
        this.counter = 0;
        this.total = 0;
        this.result = 0;
        this.flag = "N";
        log.debug("Initialized working storage variables");
    }

    /**
     * Process data through loop with calculations.
     * Equivalent to PROCESS-DATA paragraph.
     */
    public void processData() {
        for (this.counter = 1; this.counter <= 10; this.counter++) {
            this.result = this.counter * 2;
            this.total += this.result;
            if (this.total > 50) {
                this.flag = "Y";
            }
        }
        log.debug("Processed data: total={}, flag={}", this.total, this.flag);
    }

    /**
     * Display result based on flag value.
     * Equivalent to DISPLAY-RESULT paragraph.
     */
    public void displayResult() {
        if ("Y".equals(this.flag)) {
            this.output = "Total exceeded 50";
        } else {
            this.output = "Total within limit";
        }
        log.info("Result: {}", this.output);
        System.out.println(this.output);
    }

    /**
     * Cleanup working storage.
     * Equivalent to CLEANUP paragraph.
     */
    public void cleanup() {
        this.counter = 0;
        this.total = 0;
        this.result = 0;
        log.debug("Cleaned up working storage");
    }

    /**
     * Get current counter value.
     */
    public int getCounter() {
        return counter;
    }

    /**
     * Get current total value.
     */
    public long getTotal() {
        return total;
    }

    /**
     * Get current result value.
     */
    public long getResult() {
        return result;
    }

    /**
     * Get current flag value.
     */
    public String getFlag() {
        return flag;
    }

    /**
     * Get current output message.
     */
    public String getOutput() {
        return output;
    }
}
