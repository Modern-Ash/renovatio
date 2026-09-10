package org.shark.renovatio.provider.cobol.pipeline;

import java.nio.file.Path;

/**
 * Service for compiling generated Java code using Maven.
 */
public interface MavenBuildService {
    
    /**
     * Compile Java source files in the specified directory.
     * 
     * @param sourceDir Directory containing Java source files
     * @param classpath Maven classpath for compilation
     * @return Build result with success status and any errors
     */
    BuildResult compile(Path sourceDir, String classpath);
    
    /**
     * Run tests in the specified directory.
     * 
     * @param testDir Directory containing test files
     * @param classpath Maven classpath for test execution
     * @return Build result with test results
     */
    BuildResult test(Path testDir, String classpath);
    
    /**
     * Result of a Maven build operation.
     */
    record BuildResult(
        boolean success,
        int exitCode,
        String output,
        String errors,
        long compilationTimeMs
    ) {
        /**
         * Create a successful build result.
         */
        public static BuildResult success(String output, long compilationTimeMs) {
            return new BuildResult(true, 0, output, null, compilationTimeMs);
        }
        
        /**
         * Create a failed build result.
         */
        public static BuildResult failure(int exitCode, String output, String errors) {
            return new BuildResult(false, exitCode, output, errors, 0);
        }
    }
}
