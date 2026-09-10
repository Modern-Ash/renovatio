package org.shark.renovatio.provider.cobol.pipeline;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates that pipeline execution is idempotent.
 * Running the same pipeline twice produces identical output.
 */
public class IdempotencyValidator {
    
    private final MetadataNormalizer normalizer = new MetadataNormalizer();
    
    /**
     * Validate idempotency by running the pipeline twice.
     * 
     * @param pipeline Pipeline orchestrator to execute
     * @param request Pipeline request
     * @return Idempotency report
     */
    public IdempotencyReport validate(PipelineOrchestrator pipeline, PipelineRequest request) {
        // Run pipeline twice
        PipelineResult result1 = pipeline.execute(request);
        PipelineResult result2 = pipeline.execute(request);
        
        // Compare outputs
        List<Divergence> divergences = compareResults(result1, result2, request.outputDir());
        
        return new IdempotencyReport(
            request.fixtureId(),
            divergences.isEmpty(),
            divergences,
            result1,
            result2
        );
    }
    
    /**
     * Validate that stale source hash is detected.
     * 
     * @param pipeline Pipeline orchestrator
     * @param request Original pipeline request
     * @param modifiedRequest Request with modified source
     * @return Idempotency report with stale detection result
     */
    public IdempotencyReport validateStaleDetection(PipelineOrchestrator pipeline, 
                                                     PipelineRequest request,
                                                     PipelineRequest modifiedRequest) {
        // Run original pipeline
        PipelineResult result1 = pipeline.execute(request);
        
        // Run with modified source
        PipelineResult result2 = pipeline.execute(modifiedRequest);
        
        // Check if stale detection worked
        boolean staleDetected = result2.hasBlockingGaps() || 
                               result2.semanticGaps().stream()
                                   .anyMatch(gap -> gap.description().contains("stale") || 
                                                   gap.description().contains("hash"));
        
        List<Divergence> divergences = new ArrayList<>();
        if (!staleDetected) {
            divergences.add(new Divergence(
                "stale-detection",
                "expected stale detection",
                "stale source not detected",
                Divergence.Type.CONTENT
            ));
        }
        
        return new IdempotencyReport(
            request.fixtureId(),
            staleDetected && divergences.isEmpty(),
            divergences,
            result1,
            result2
        );
    }
    
    private List<Divergence> compareResults(PipelineResult result1, PipelineResult result2, Path outputDir) {
        List<Divergence> divergences = new ArrayList<>();
        
        // Compare stage outputs
        divergences.addAll(compareStageOutput("discover", result1.discover(), result2.discover()));
        divergences.addAll(compareStageOutput("parse", result1.parse(), result2.parse()));
        divergences.addAll(compareStageOutput("semanticIr", result1.semanticIr(), result2.semanticIr()));
        divergences.addAll(compareStageOutput("decisions", result1.decisions(), result2.decisions()));
        divergences.addAll(compareStageOutput("manifest", result1.manifest(), result2.manifest()));
        divergences.addAll(compareStageOutput("emit", result1.emit(), result2.emit()));
        divergences.addAll(compareStageOutput("openRewrite", result1.openRewrite(), result2.openRewrite()));
        divergences.addAll(compareStageOutput("build", result1.build(), result2.build()));
        
        return divergences;
    }
    
    private List<Divergence> compareStageOutput(String stageName, StageResult result1, StageResult result2) {
        List<Divergence> divergences = new ArrayList<>();
        
        if (result1.output() != null && result2.output() != null) {
            String normalized1 = normalizer.normalize(result1.output());
            String normalized2 = normalizer.normalize(result2.output());
            
            if (!normalized1.equals(normalized2)) {
                divergences.add(new Divergence(
                    stageName + ".output",
                    normalized1,
                    normalized2,
                    Divergence.Type.CONTENT
                ));
            }
        }
        
        return divergences;
    }
    
    /**
     * Report from idempotency validation.
     */
    public record IdempotencyReport(
        String fixtureId,
        boolean idempotent,
        List<Divergence> divergences,
        PipelineResult firstRun,
        PipelineResult secondRun
    ) {
        /**
         * Check if the pipeline is idempotent.
         */
        public boolean isIdempotent() {
            return idempotent;
        }
    }
    
    /**
     * Represents a divergence between two runs.
     */
    public record Divergence(
        String location,
        String value1,
        String value2,
        Type type
    ) {
        public enum Type {
            CONTENT,
            WHITESPACE,
            ORDERING,
            METADATA
        }
    }
}
