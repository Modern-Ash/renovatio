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
public class RunDto {
    private String runId;
    private String planId;
    private Boolean dryRun;
    private String diff;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
