package org.shark.renovatio.api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "run_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RunSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String projectId;

    @Column(nullable = false)
    private String runId;

    private String planId;

    @Builder.Default
    private Boolean dryRun = false;

    @Column(columnDefinition = "TEXT")
    private String diffJson;

    @Column(columnDefinition = "TEXT")
    private String resultJson;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
    }
}
