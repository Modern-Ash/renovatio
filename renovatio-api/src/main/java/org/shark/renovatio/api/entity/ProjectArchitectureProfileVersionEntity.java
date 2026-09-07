package org.shark.renovatio.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_architecture_profile_versions", uniqueConstraints =
        @UniqueConstraint(name = "uk_architecture_profile_project_revision", columnNames = { "project_id", "revision" }))
public class ProjectArchitectureProfileVersionEntity {
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
    @Column(name = "profile_json", nullable = false, columnDefinition = "TEXT")
    private String profileJson;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    protected ProjectArchitectureProfileVersionEntity() { }

    public ProjectArchitectureProfileVersionEntity(String projectId, long revision, String canonicalHash,
                                                   String profileJson, LocalDateTime savedAt) {
        this.id = projectId + "::architecture::" + revision;
        this.projectId = projectId;
        this.revision = revision;
        this.canonicalHash = canonicalHash;
        this.profileJson = profileJson;
        this.savedAt = savedAt;
    }

    public String getProjectId() { return projectId; }
    public long getRevision() { return revision; }
    public String getCanonicalHash() { return canonicalHash; }
    public String getProfileJson() { return profileJson; }
    public LocalDateTime getSavedAt() { return savedAt; }
}
