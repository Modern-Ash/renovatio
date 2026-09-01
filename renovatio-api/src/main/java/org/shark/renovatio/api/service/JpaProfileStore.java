package org.shark.renovatio.api.service;

import org.shark.renovatio.api.entity.ProjectProfileEntity;
import org.shark.renovatio.api.repository.ProjectProfileRepository;
import org.shark.renovatio.decisions.ProfileStore;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class JpaProfileStore implements ProfileStore {
    private final ProjectProfileRepository repository;
    public JpaProfileStore(ProjectProfileRepository repository) { this.repository = repository; }

    @Override public Optional<VersionedProfile> find(String projectId) {
        return repository.findById(projectId).map(value -> new VersionedProfile(
                MigrationProfiles.readJson(value.getOverlayJson()), value.getProfileRevision()));
    }

    @Override @Transactional
    public VersionedProfile replace(String projectId, MigrationProfile profile, long expectedRevision) {
        String encoded = MigrationProfiles.writeJson(profile);
        ProjectProfileEntity entity = repository.findById(projectId).orElse(null);
        long current = entity == null ? 0 : entity.getProfileRevision();
        if (current != expectedRevision) throw new ProfileConflictException();
        if (entity != null && entity.getOverlayJson().equals(encoded)) return new VersionedProfile(profile, current);
        if (entity == null) entity = new ProjectProfileEntity(projectId, "1", encoded, 1);
        else entity.replace(encoded);
        ProjectProfileEntity saved = repository.save(entity);
        return new VersionedProfile(profile, saved.getProfileRevision());
    }
    @Override public void deleteProject(String projectId) { repository.deleteById(projectId); }
    public static final class ProfileConflictException extends IllegalArgumentException { }
}
