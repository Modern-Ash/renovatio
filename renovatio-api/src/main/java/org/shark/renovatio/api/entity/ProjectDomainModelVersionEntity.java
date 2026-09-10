package org.shark.renovatio.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_domain_model_versions", uniqueConstraints =
        @UniqueConstraint(name = "uk_domain_model_project_revision", columnNames = { "project_id", "revision" }))
public class ProjectDomainModelVersionEntity {
    @Id
    @Column(nullable = false, length = 512)
    private String id;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(nullable = false)
    private long revision;

    @Column(name = "canonical_hash", nullable = false, length = 64)
    private String canonicalHash;

    @Lob
    @Column(name = "model_json", nullable = false, columnDefinition = "TEXT")
    private String modelJson;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    protected ProjectDomainModelVersionEntity() { }

    public ProjectDomainModelVersionEntity(String projectId, long revision, String canonicalHash,
                                           String modelJson, LocalDateTime savedAt) {
        this.id = projectId + "::" + revision;
        this.projectId = projectId;
        this.revision = revision;
        this.canonicalHash = canonicalHash;
        this.modelJson = modelJson;
        this.savedAt = savedAt;
    }

    public String getProjectId() { return projectId; }
    public long getRevision() { return revision; }
    public String getCanonicalHash() { return canonicalHash; }
    public String getModelJson() { return modelJson; }
    public LocalDateTime getSavedAt() { return savedAt; }
}
