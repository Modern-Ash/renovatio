package org.shark.renovatio.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ProjectDecisionId implements Serializable {
    @Column(name = "project_id", nullable = false)
    private String projectId;
    @Column(name = "decision_id", nullable = false, length = 64)
    private String decisionId;

    protected ProjectDecisionId() { }
    public ProjectDecisionId(String projectId, String decisionId) {
        this.projectId = projectId; this.decisionId = decisionId;
    }
    public String getProjectId() { return projectId; }
    public String getDecisionId() { return decisionId; }
    @Override public boolean equals(Object other) {
        return other instanceof ProjectDecisionId value
                && Objects.equals(projectId, value.projectId) && Objects.equals(decisionId, value.decisionId);
    }
    @Override public int hashCode() { return Objects.hash(projectId, decisionId); }
}
