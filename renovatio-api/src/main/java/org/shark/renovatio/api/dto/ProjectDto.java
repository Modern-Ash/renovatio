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
public class ProjectDto {
    private String id;
    private String name;
    private String workspacePath;
    private String branch;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
