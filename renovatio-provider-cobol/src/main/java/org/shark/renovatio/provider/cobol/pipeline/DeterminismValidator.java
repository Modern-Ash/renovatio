package org.shark.renovatio.provider.cobol.pipeline;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates that pipeline execution is deterministic.
 * Runs the pipeline multiple times and compares outputs.
 */
public class DeterminismValidator {
    
    private final MetadataNormalizer normalizer = new MetadataNormalizer();
    
    /**
     * Validate determinism by running the pipeline multiple times.
     * 
     * @param pipeline Pipeline orchestrator to execute
     * @param request Pipeline request
     * @param runs Number of runs to compare
     * @return Determinism report
     */
    public DeterminismReport validate(PipelineOrchestrator pipeline, PipelineRequest request, int runs) {
        List<PipelineResult> results = new ArrayList<>();
        
        // Execute pipeline multiple times
        for (int i = 0; i < runs; i++) {
            PipelineResult result = pipeline.execute(request);
            results.add(result);
        }
        
        // Compare outputs byte-by-byte
        List<Divergence> divergences = new ArrayList<>();
        for (int i = 1; i < results.size(); i++) {
            List<Divergence> runDivergences = compareResults(
                results.get(0), results.get(i), request.outputDir()
            );
            divergences.addAll(runDivergences);
        }
        
        return new DeterminismReport(
            request.fixtureId(),
            runs,
            divergences.isEmpty(),
            divergences,
            results.get(0)
        );
    }
    
    private List<Divergence> compareResults(PipelineResult result1, PipelineResult result2, Path outputDir) {
        List<Divergence> divergences = new ArrayList<>();
        
        // Compare stage results
        divergences.addAll(compareStageResults("discover", result1.discover(), result2.discover()));
        divergences.addAll(compareStageResults("parse", result1.parse(), result2.parse()));
        divergences.addAll(compareStageResults("semanticIr", result1.semanticIr(), result2.semanticIr()));
        divergences.addAll(compareStageResults("decisions", result1.decisions(), result2.decisions()));
        divergences.addAll(compareStageResults("manifest", result1.manifest(), result2.manifest()));
        divergences.addAll(compareStageResults("emit", result1.emit(), result2.emit()));
        divergences.addAll(compareStageResults("openRewrite", result1.openRewrite(), result2.openRewrite()));
        divergences.addAll(compareStageResults("build", result1.build(), result2.build()));
        
        return divergences;
    }
    
    private List<Divergence> compareStageResults(String stageName, StageResult result1, StageResult result2) {
        List<Divergence> divergences = new ArrayList<>();
        
        if (result1.success() != result2.success()) {
            divergences.add(new Divergence(
                stageName + ".success",
                String.valueOf(result1.success()),
                String.valueOf(result2.success()),
                Divergence.Type.CONTENT
            ));
        }
        
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
     * Report from determinism validation.
     */
    public record DeterminismReport(
        String fixtureId,
        int runs,
        boolean deterministic,
        List<Divergence> divergences,
        PipelineResult sampleResult
    ) {
        /**
         * Check if the pipeline is deterministic.
         */
        public boolean isDeterministic() {
            return deterministic;
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
