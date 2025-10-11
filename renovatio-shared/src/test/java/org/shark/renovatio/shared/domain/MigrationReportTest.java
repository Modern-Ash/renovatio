package org.shark.renovatio.shared.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MigrationReportTest {

    @Test
    void addMetrics_and_addStatus_shouldAccumulate_andRecord() {
        MigrationReport r = new MigrationReport();
        r.addMetrics(Map.of("files", 2, "lines", 10.5));
        r.addMetrics(Map.of("files", 3, "errors", 1));
        assertEquals(5.0, r.getMetrics().get("files"));
        assertEquals(10.5, r.getMetrics().get("lines"));
        assertEquals(1.0, r.getMetrics().get("errors"));

        r.addStatus("core", true);
        r.addStatus("cobol", false);
        assertEquals("SUCCESS", r.getStatuses().get("core"));
        assertEquals("FAILED", r.getStatuses().get("cobol"));

        // Null-safe addMetrics
        r.addMetrics(null);
        assertEquals(3, r.getMetrics().size());
    }
}

