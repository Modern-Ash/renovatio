package org.shark.renovatio.profile;

/** Resolves the immutable F1 decision/profile envelope for one project boundary. */
@FunctionalInterface
public interface EffectiveProfileResolver {
    MigrationProfiles.EffectiveProfile resolve(String projectId);
}
