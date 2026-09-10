package org.shark.renovatio.provider.cobol.pipeline;

/**
 * Orchestrates the complete COBOL-to-Java migration pipeline.
 * 
 * The pipeline consists of 10 capabilities:
 * 1. Discover - Find COBOL files in fixture directory
 * 2. Parse - Parse COBOL source into intermediate model
 * 3. Semantic IR - Transform to semantic intermediate representation
 * 4. Domain Model - Project target-neutral business concepts
 * 5. Decisions - Resolve the effective migration profile and decisions
 * 6. Architecture - Project the canonical architecture model
 * 7. Manifest - Produce the canonical artifact manifest
 * 8. Emit - Write target files; the Java emitter owns OpenRewrite enrichment
 * 9. Build - Compile generated Java code
 * 10. Equivalence - Compare complete actual and expected file sets (optional)
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
