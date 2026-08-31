package org.shark.renovatio.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionItemDto {
    private String id;
    private String projectId;
    private String runId;
    private String severity;
    private String reason;
    private String requiredHumanAction;
    private String acceptanceCondition;
    private String reviewStatus;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
