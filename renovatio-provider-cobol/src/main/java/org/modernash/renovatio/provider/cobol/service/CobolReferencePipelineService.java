package org.modernash.renovatio.provider.cobol.service;

import org.modernash.renovatio.provider.cobol.pipeline.PipelineOrchestrator;
import org.modernash.renovatio.provider.cobol.pipeline.PipelineRequest;
import org.modernash.renovatio.provider.cobol.pipeline.PipelineResult;
import org.springframework.stereotype.Service;

import java.util.Objects;

/** Application-facing entry point for the governed COBOL reference pipeline. */
@Service
public class CobolReferencePipelineService {
    private final PipelineOrchestrator pipeline;

    public CobolReferencePipelineService(PipelineOrchestrator pipeline) {
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
    }

    public PipelineResult execute(PipelineRequest request) {
        return pipeline.execute(request);
    }
}
