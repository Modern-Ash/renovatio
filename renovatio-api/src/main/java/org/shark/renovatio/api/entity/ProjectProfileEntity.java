package org.shark.renovatio.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "project_profiles")
public class ProjectProfileEntity {
    @Id
    @Column(name = "project_id", nullable = false)
    private String projectId;
    @Column(name = "schema_version", nullable = false, length = 16)
    private String schemaVersion;
    @Lob
    @Column(name = "overlay_json", nullable = false)
    private String overlayJson;
    @Column(name = "profile_revision", nullable = false)
    private long profileRevision;
    @Version
    private long lockVersion;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected ProjectProfileEntity() { }

    public ProjectProfileEntity(String projectId, String schemaVersion, String overlayJson, long profileRevision) {
        this.projectId = projectId;
        this.schemaVersion = schemaVersion;
        this.overlayJson = overlayJson;
        this.profileRevision = profileRevision;
    }

    @PrePersist void created() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void updated() { updatedAt = Instant.now(); }

    public String getProjectId() { return projectId; }
    public String getOverlayJson() { return overlayJson; }
    public long getProfileRevision() { return profileRevision; }
    public void replace(String value) { overlayJson = value; profileRevision++; }
}
