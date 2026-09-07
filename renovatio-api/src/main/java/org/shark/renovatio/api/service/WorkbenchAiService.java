package org.shark.renovatio.api.service;

import org.shark.renovatio.api.dto.WorkbenchAiDto;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class WorkbenchAiService {
    private final DecisionLayerService decisions;
    public WorkbenchAiService(DecisionLayerService decisions) { this.decisions = decisions; }
    public WorkbenchAiDto summary(String projectId) {
        try {
            return new WorkbenchAiDto(decisions.decisions(projectId, null, null, null).stream()
                    .map(value -> new WorkbenchAiDto.Item(value.id(), value.category().name(), value.source().name(), value.status().name(), value.confidence(), value.evidence().size(), value.llmFailed())).toList());
        } catch (RuntimeException exception) {
            return new WorkbenchAiDto(List.of());
        }
    }
}
