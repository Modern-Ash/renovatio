package org.shark.renovatio.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

@Entity
@Table(name = "project_decisions", indexes = {
        @Index(name = "idx_decision_category", columnList = "project_id,active,category"),
        @Index(name = "idx_decision_status", columnList = "project_id,active,status"),
        @Index(name = "idx_decision_confidence", columnList = "project_id,active,confidence")
})
public class ProjectDecisionEntity {
    @EmbeddedId
    private ProjectDecisionId id;
    @Column(nullable = false, length = 32)
    private String category;
    @Column(name = "decision_key", nullable = false, length = 128)
    private String decisionKey;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(nullable = false, precision = 6, scale = 5)
    private BigDecimal confidence;
    @Column(nullable = false)
    private boolean active;
    @Lob
    @Column(name = "decision_json", nullable = false)
    private String decisionJson;
    @Version
    private long lockVersion;

    protected ProjectDecisionEntity() { }
    public ProjectDecisionEntity(ProjectDecisionId id) { this.id = id; }
    public ProjectDecisionId getId() { return id; }
    public String getDecisionJson() { return decisionJson; }
    public String getCategory() { return category; }
    public String getStatus() { return status; }
    public BigDecimal getConfidence() { return confidence; }
    public boolean isActive() { return active; }
    public void update(String category, String decisionKey, String status, BigDecimal confidence,
                       boolean active, String decisionJson) {
        this.category = category; this.decisionKey = decisionKey; this.status = status;
        this.confidence = confidence; this.active = active; this.decisionJson = decisionJson;
    }
}
