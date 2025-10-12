package org.shark.renovatio.shared.util;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.shared.domain.AnalyzeResult;
import org.shark.renovatio.shared.domain.PerformanceMetrics;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchmarkUtilsTest {

    private static void run() {
        // trivial work
        for (int i = 0; i < 10; i++) {
        }
    }

    @Test
    void measure_returnsExecutionTime() {
        PerformanceMetrics pm = BenchmarkUtils.measure(BenchmarkUtilsTest::run);
        assertTrue(pm.getExecutionTimeMs() >= 0);
    }

    @Test
    void compare_handles_missing_metrics() {
        AnalyzeResult baseline = new AnalyzeResult(true, "b");
        AnalyzeResult migrated = new AnalyzeResult(true, "m");
        String out = BenchmarkUtils.compare(baseline, migrated);
        assertTrue(out.contains("not available"));
    }

    @Test
    void compare_formats_summary() {
        AnalyzeResult baseline = new AnalyzeResult(true, "b");
        baseline.setPerformance(new PerformanceMetrics(100));
        AnalyzeResult migrated = new AnalyzeResult(true, "m");
        migrated.setPerformance(new PerformanceMetrics(120));
        String out = BenchmarkUtils.compare(baseline, migrated);
        assertTrue(out.contains("Baseline: 100 ms"));
        assertTrue(out.contains("Migrated: 120 ms"));
    }
}

