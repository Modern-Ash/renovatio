package org.shark.renovatio.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "domain_suggestion_decisions")
public class DomainSuggestionDecisionEntity {
    @Id
    @Column(nullable = false, length = 768)
    private String id;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "suggestion_id", nullable = false, length = 512)
    private String suggestionId;

    @Column(nullable = false, length = 16)
    private String action;

    @Column(name = "model_revision", nullable = false)
    private long modelRevision;

    @Column(name = "decided_at", nullable = false)
    private LocalDateTime decidedAt;

    protected DomainSuggestionDecisionEntity() { }

    public DomainSuggestionDecisionEntity(String projectId, String suggestionId, String action,
                                          long modelRevision, LocalDateTime decidedAt) {
        this.id = projectId + "::" + suggestionId;
        this.projectId = projectId;
        this.suggestionId = suggestionId;
        this.action = action;
        this.modelRevision = modelRevision;
        this.decidedAt = decidedAt;
    }

    public String getSuggestionId() { return suggestionId; }
    public String getAction() { return action; }
    public long getModelRevision() { return modelRevision; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
}
