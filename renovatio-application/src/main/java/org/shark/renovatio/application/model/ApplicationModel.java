package org.shark.renovatio.application.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Immutable, transport-neutral values exchanged by the application boundary. */
public final class ApplicationModel {
    private ApplicationModel() { }

    public record Project(String id, String name, Instant createdAt, Map<String, String> metadata) {
        public Project {
            require(id, "project id");
            require(name, "project name");
            Objects.requireNonNull(createdAt, "createdAt");
            metadata = immutable(metadata);
        }
    }

    public record SourceSnapshot(String projectId, String hash, Map<String, byte[]> files) {
        public SourceSnapshot {
            require(projectId, "projectId");
            files = immutableBytes(files);
            String computed = hashFiles(files);
            if (hash == null || hash.isBlank()) hash = computed;
            if (!hash.equals(computed)) throw new IllegalArgumentException("source hash does not match files");
        }
        @Override public Map<String, byte[]> files() { return immutableBytes(files); }
    }

    public record Analysis(String sourceHash, Object semanticModel, List<String> evidence) {
        public Analysis {
            require(sourceHash, "sourceHash");
            Objects.requireNonNull(semanticModel, "semanticModel");
            evidence = List.copyOf(evidence == null ? List.of() : evidence);
        }
    }

    public record DomainReview(Object domainModel, List<String> proposals) {
        public DomainReview {
            Objects.requireNonNull(domainModel, "domainModel");
            proposals = List.copyOf(proposals == null ? List.of() : proposals);
        }
    }

    public record DecisionResolution(Map<String, String> decisions) {
        public DecisionResolution { decisions = immutable(decisions); }
    }

    public record MigrationPlan(String id, String projectId, String sourceHash, Object projection,
                                Map<String, String> decisions) {
        public MigrationPlan {
            require(id, "plan id"); require(projectId, "projectId"); require(sourceHash, "sourceHash");
            Objects.requireNonNull(projection, "projection"); decisions = immutable(decisions);
        }
    }

    public record ArtifactManifest(String id, String projectId, String sourceHash,
                                   Map<String, byte[]> artifacts) {
        public ArtifactManifest {
            require(projectId, "projectId"); require(sourceHash, "sourceHash");
            artifacts = immutableBytes(artifacts);
            String computed = hashManifest(projectId, sourceHash, artifacts);
            if (id == null || id.isBlank()) id = computed;
            if (!id.equals(computed)) throw new IllegalArgumentException("manifest id does not match content");
        }
        @Override public Map<String, byte[]> artifacts() { return immutableBytes(artifacts); }
    }

    public record ValidationResult(boolean valid, List<String> evidence) {
        public ValidationResult { evidence = List.copyOf(evidence == null ? List.of() : evidence); }
    }

    public enum ChangeState { PREPARED, APPLIED, REVERTED }

    public record ChangeSet(String id, String projectId, String manifestId, Map<String, byte[]> preimage,
                            Map<String, byte[]> postimage, ChangeState state, List<String> evidence,
                            Instant occurredAt) {
        public ChangeSet {
            require(id, "change set id"); require(projectId, "projectId"); require(manifestId, "manifestId");
            preimage = immutableBytes(preimage); postimage = immutableBytes(postimage);
            Objects.requireNonNull(state, "state");
            evidence = List.copyOf(evidence == null ? List.of() : evidence);
            Objects.requireNonNull(occurredAt, "occurredAt");
        }
        @Override public Map<String, byte[]> preimage() { return immutableBytes(preimage); }
        @Override public Map<String, byte[]> postimage() { return immutableBytes(postimage); }
        public ChangeSet withState(ChangeState next, List<String> nextEvidence) {
            return new ChangeSet(id, projectId, manifestId, preimage, postimage, next, nextEvidence, occurredAt);
        }
    }

    public record IdempotencyRecord(String projectId, String operation, String key, String commandDigest,
                                    Object result, boolean successful) {
        public IdempotencyRecord {
            require(projectId, "projectId"); require(operation, "operation"); require(key, "key");
            require(commandDigest, "commandDigest"); Objects.requireNonNull(result, "result");
        }
    }

    public record CreateProject(String id, String name, String idempotencyKey, Map<String, String> metadata) { }
    public record AnalyzeProject(String projectId) { }
    public record ReviewDomain(String projectId) { }
    public record ResolveDecisions(String projectId, String idempotencyKey, Map<String, String> decisions) { }
    public record Plan(String projectId) { }
    public record Preview(String projectId, String planId) { }
    public record Validate(String projectId, String manifestId) { }
    public record Apply(String projectId, String manifestId, String expectedSourceHash,
                        String idempotencyKey) { }
    public record ExportEvidence(String projectId) { }

    public static String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder("sha256:");
            for (byte valueByte : bytes) result.append(String.format("%02x", valueByte));
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    public static String hashFiles(Map<String, byte[]> files) {
        StringBuilder canonical = new StringBuilder();
        new TreeMap<>(files == null ? Map.of() : files).forEach((path, content) ->
                canonical.append(path).append('\n').append(digestBytes(content)).append('\n'));
        return digest(canonical.toString());
    }

    private static String hashManifest(String projectId, String sourceHash, Map<String, byte[]> files) {
        return digest(projectId + "\n" + sourceHash + "\n" + hashFiles(files));
    }

    private static String digestBytes(byte[] value) {
        return digest(new String(value == null ? new byte[0] : value, StandardCharsets.ISO_8859_1));
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
    }

    private static <K, V> Map<K, V> immutable(Map<K, V> value) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(value == null ? Map.of() : value));
    }

    private static Map<String, byte[]> immutableBytes(Map<String, byte[]> value) {
        Map<String, byte[]> copy = new TreeMap<>();
        (value == null ? Map.<String, byte[]>of() : value).forEach((key, bytes) ->
                copy.put(key, (bytes == null ? new byte[0] : bytes).clone()));
        return Collections.unmodifiableMap(copy);
    }
}
