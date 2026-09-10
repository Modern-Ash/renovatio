package org.shark.renovatio.provider.cobol.pipeline;

import java.nio.file.Path;
import java.util.List;

/**
 * Service for checking equivalence between generated and expected output.
 */
public interface EquivalenceChecker {
    
    /**
     * Check equivalence between actual and expected files.
     * 
     * @param actualPath Path to actual generated file
     * @param expectedPath Path to expected golden file
     * @param config Equivalence configuration
     * @return Equivalence report with divergences and gate decision
     */
    EquivalenceReport check(Path actualPath, Path expectedPath, EquivalenceConfig config);
    
    /**
     * Check equivalence for all files in a fixture.
     * 
     * @param fixtureId Fixture identifier
     * @param actualDir Directory with actual generated files
     * @param expectedDir Directory with expected golden files
     * @param config Equivalence configuration
     * @return List of equivalence reports, one per file
     */
    List<EquivalenceReport> checkAll(String fixtureId, Path actualDir, Path expectedDir, 
                                     EquivalenceConfig config);
    
    /**
     * Configuration for equivalence checking.
     */
    record EquivalenceConfig(
        List<String> excludedMetadataPatterns,
        List<String> ignoredDirectories,
        boolean ignoreWhitespace,
        boolean ignoreImportOrder,
        List<DivergenceRule> divergenceRules
    ) {
        /**
         * Default configuration with standard exclusions.
         */
        public static EquivalenceConfig defaults() {
            return new EquivalenceConfig(
                List.of(
                    "Generated at.*",
                    "Source hash.*",
                    "Path:.*"
                ),
                List.of(),
                true,
                true,
                List.of()
            );
        }
        
        /**
         * Strict configuration without exclusions.
         */
        public static EquivalenceConfig strict() {
            return new EquivalenceConfig(
                List.of(),
                List.of(),
                false,
                false,
                List.of()
            );
        }
    }
    
    /**
     * Rule for determining if a divergence is acceptable.
     */
    record DivergenceRule(
        String pattern,
        DivergenceType type,
        boolean acceptable
    ) {
        public enum DivergenceType {
            WHITESPACE,
            ORDERING,
            CONTENT,
            METADATA
        }
    }
}
