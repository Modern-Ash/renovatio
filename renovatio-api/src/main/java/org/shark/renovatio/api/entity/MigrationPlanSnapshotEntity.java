package org.shark.renovatio.api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "plan_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MigrationPlanSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String projectId;

    @Column(nullable = false)
    private String planId;

    @Column(columnDefinition = "TEXT")
    private String planContentJson;

    @Column(columnDefinition = "TEXT")
    private String stepsJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
