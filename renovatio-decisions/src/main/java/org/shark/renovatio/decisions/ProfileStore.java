package org.shark.renovatio.decisions;

import org.shark.renovatio.profile.MigrationProfile;

import java.util.Optional;

/** Project-scoped profile overlay persistence port. */
public interface ProfileStore {
    Optional<VersionedProfile> find(String projectId);
    VersionedProfile replace(String projectId, MigrationProfile profile, long expectedRevision);
    void deleteProject(String projectId);

    record VersionedProfile(MigrationProfile profile, long revision) { }
}
