package org.shark.renovatio.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "project_domain_models")
public class ProjectDomainModelEntity {
    @Id @Column(name = "project_id", nullable = false) private String projectId;
    @Column(name = "schema_version", nullable = false) private String schemaVersion;
    @Column(name = "canonical_hash", nullable = false, length = 64) private String canonicalHash;
    @Lob @Column(name = "model_json", nullable = false) private String modelJson;
    @Column(nullable = false) private boolean confirmed;
    @Column(nullable = false) private Instant updatedAt;
    protected ProjectDomainModelEntity() { }
    public ProjectDomainModelEntity(String projectId, String schemaVersion, String canonicalHash, String modelJson) {
        this.projectId = projectId; this.schemaVersion = schemaVersion; this.canonicalHash = canonicalHash; this.modelJson = modelJson;
    }
    @PrePersist @PreUpdate void touch() { updatedAt = Instant.now(); }
    public String getProjectId() { return projectId; }
    public String getModelJson() { return modelJson; }
    public boolean isConfirmed() { return confirmed; }
    public void replace(String hash, String json) { canonicalHash = hash; modelJson = json; }
    public void confirm() { confirmed = true; }
}
