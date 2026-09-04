package org.shark.renovatio.profile;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Template creation, application, and deterministic leaf-level comparison. */
public final class ProfileTemplates {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    private ProfileTemplates() { }

    public static MigrationProfileTemplate snapshot(String name, String version, String description,
                                                    MigrationProfile projectOverlay, Instant now) {
        return MigrationProfileTemplate.create(name, version, description, projectOverlay, now);
    }

    public static MigrationProfile effective(MigrationProfileTemplate template, MigrationProfile projectOverlay) {
        return MigrationProfiles.overlay(MigrationProfiles.resolve(template.profile()), projectOverlay);
    }

    public static List<ProfileDiff> diff(MigrationProfileTemplate template, MigrationProfile projectOverlay) {
        MigrationProfile baseProfile = MigrationProfiles.resolve(template.profile());
        Map<String, Object> base = flatten(baseProfile);
        Map<String, Object> project = flatten(MigrationProfiles.overlay(baseProfile, projectOverlay));
        List<String> paths = new ArrayList<>();
        paths.addAll(base.keySet());
        project.keySet().stream().filter(path -> !base.containsKey(path)).forEach(paths::add);
        return paths.stream().distinct().sorted().filter(path -> !java.util.Objects.equals(base.get(path), project.get(path)))
                .map(path -> new ProfileDiff(path, base.get(path), project.get(path),
                        !base.containsKey(path) ? ChangeKind.ADDED : !project.containsKey(path) ? ChangeKind.REMOVED : ChangeKind.CHANGED))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> flatten(MigrationProfile profile) {
        Map<String, Object> result = new LinkedHashMap<>();
        flatten("", JSON.convertValue(profile, Map.class), result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void flatten(String prefix, Object value, Map<String, Object> result) {
        if (value instanceof Map<?, ?> map) {
            map.entrySet().stream().sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                    .forEach(entry -> flatten(prefix + "/" + entry.getKey(), entry.getValue(), result));
        } else {
            result.put(prefix, value);
        }
    }

    public record ProfileDiff(String path, Object templateValue, Object projectValue, ChangeKind changeKind) { }
    public enum ChangeKind { ADDED, REMOVED, CHANGED }
}
