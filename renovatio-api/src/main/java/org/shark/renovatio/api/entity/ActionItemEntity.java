package org.shark.renovatio.api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "action_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String projectId;

    private String runId;

    @Column(nullable = false)
    private String severity;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String requiredHumanAction;

    @Column(columnDefinition = "TEXT")
    private String acceptanceCondition;

    @Builder.Default
    private String reviewStatus = "PENDING";

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
