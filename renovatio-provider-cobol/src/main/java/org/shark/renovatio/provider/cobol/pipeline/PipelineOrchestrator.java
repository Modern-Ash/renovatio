package org.shark.renovatio.provider.cobol.pipeline;

/**
 * Orchestrates the complete COBOL-to-Java migration pipeline.
 * 
 * The pipeline consists of 7 stages:
 * 1. Discover - Find COBOL files in fixture directory
 * 2. Parse - Parse COBOL source into intermediate model
 * 3. Semantic IR - Transform to semantic intermediate representation
 * 4. Decisions - Apply migration decisions from configuration
 * 5. Manifest - Generate Java code structure
 * 6. Emit - Write Java files to disk
 * 7. OpenRewrite - Apply OpenRewrite recipes for enrichment
 * 8. Build - Compile generated Java code
 * 9. Equivalence - Compare against expected output (optional)
 */
public interface PipelineOrchestrator {
    
    /**
     * Execute the complete pipeline for a single fixture.
     * 
     * @param request Pipeline request with fixture details
     * @return Pipeline result with stage outcomes and equivalence report
     */
    PipelineResult execute(PipelineRequest request);
    
    /**
     * Execute the pipeline for multiple fixtures.
     * 
     * @param requests List of pipeline requests
     * @return List of pipeline results
     */
    java.util.List<PipelineResult> executeAll(java.util.List<PipelineRequest> requests);
}
