package org.shark.renovatio.api.service;

import org.shark.renovatio.api.dto.WorkbenchChangeSetDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkbenchChangeSetService {
    private static final String DANGEROUS_APPROVAL = "APPROVE DANGEROUS CHANGE SET";
    private static final String APPLY_CONFIRMATION = "APPLY APPROVED CHANGE SET";
    private static final String ROLLBACK_CONFIRMATION = "ROLL BACK APPLIED CHANGE SET";
    private final WorkbenchProjectAdapterService adapter;
    private final Map<String, StoredChangeSet> changeSets = new ConcurrentHashMap<>();

    public WorkbenchChangeSetService(WorkbenchProjectAdapterService adapter) {
        this.adapter = adapter;
    }

    public List<WorkbenchChangeSetDto> list(String projectId) {
        return changeSets.values().stream()
                .filter(changeSet -> changeSet.projectId.equals(projectId))
                .sorted(Comparator.comparing(changeSet -> changeSet.createdAt))
                .map(this::dto)
                .toList();
    }

    public WorkbenchChangeSetDto get(String projectId, String id) {
        return dto(find(projectId, id));
    }

    public WorkbenchChangeSetDto create(String projectId, Path root, WorkbenchChangeSetDto.CreateRequest request,
                                        String actor) throws IOException {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new ChangeSetException("title is required");
        }
        if (request.files() == null || request.files().isEmpty()) {
            throw new ChangeSetException("at least one file change is required");
        }
        String id = "cs-" + UUID.randomUUID();
        List<StoredFileChange> files = new ArrayList<>();
        for (WorkbenchChangeSetDto.FileChangeRequest file : request.files()) {
            if (file.path() == null || file.path().isBlank()) throw new ChangeSetException("file path is required");
            String action = file.action() == null || file.action().isBlank() ? "modify" : file.action().toLowerCase();
            if (!List.of("modify", "create").contains(action)) throw new ChangeSetException("file action must be create or modify");
            String before = readOrEmpty(root, file.path());
            String proposed = file.proposedContent() == null ? "" : file.proposedContent();
            files.add(new StoredFileChange(file.path(), action, before, proposed));
        }
        StoredChangeSet stored = new StoredChangeSet(id, projectId, request.title(), State.DRAFT,
                request.dangerous(), files, safeList(request.decisions()), safeList(request.evidence()), Instant.now());
        stored.history.add(audit(actor, "created", "Draft change set created", stored.manifestHash()));
        changeSets.put(id, stored);
        return dto(stored);
    }

    public WorkbenchChangeSetDto diff(String projectId, String id) {
        StoredChangeSet stored = find(projectId, id);
        stored.diffViewed = true;
        stored.history.add(audit("system", "diff-viewed", "Mandatory diff reviewed before approval", stored.manifestHash()));
        return dto(stored);
    }

    public WorkbenchChangeSetDto submitReview(String projectId, String id, String actor) {
        StoredChangeSet stored = find(projectId, id);
        require(stored.state == State.DRAFT, "Only draft change sets can enter review");
        stored.state = State.REVIEW;
        stored.history.add(audit(actor, "submitted-review", "Change set entered review", stored.manifestHash()));
        return dto(stored);
    }

    public WorkbenchChangeSetDto approve(String projectId, String id, WorkbenchChangeSetDto.ApprovalRequest request,
                                         String actor) {
        StoredChangeSet stored = find(projectId, id);
        require(stored.state == State.REVIEW, "Only reviewed change sets can be approved");
        require(stored.diffViewed && request != null && request.diffReviewed(), "Diff must be viewed and confirmed before approval");
        if (stored.dangerous) {
            require(DANGEROUS_APPROVAL.equals(request.confirmationPhrase()),
                    "Dangerous change sets require the exact approval confirmation phrase");
        }
        stored.state = State.APPROVED;
        stored.approvedManifestHash = stored.manifestHash();
        stored.history.add(audit(actor, "approved", request.reason(), stored.approvedManifestHash));
        return dto(stored);
    }

    public WorkbenchChangeSetDto reject(String projectId, String id, WorkbenchChangeSetDto.ActionRequest request,
                                        String actor) {
        StoredChangeSet stored = find(projectId, id);
        require(stored.state == State.DRAFT || stored.state == State.REVIEW, "Only draft or review change sets can be rejected");
        stored.state = State.REJECTED;
        stored.history.add(audit(actor, "rejected", request == null ? null : request.reason(), stored.manifestHash()));
        return dto(stored);
    }

    public WorkbenchChangeSetDto apply(String projectId, String id, Path root, WorkbenchChangeSetDto.ActionRequest request,
                                       String actor, boolean devWriteEnabled) throws IOException {
        StoredChangeSet stored = find(projectId, id);
        require(stored.state == State.APPROVED, "Only approved change sets can be applied");
        require(APPLY_CONFIRMATION.equals(request == null ? null : request.confirmationPhrase()),
                "Applying requires the exact confirmation phrase");
        require(stored.manifestHash().equals(stored.approvedManifestHash), "Approved manifest no longer matches the change set");
        for (StoredFileChange file : stored.files) {
            try {
                adapter.write(root, file.path, file.after, devWriteEnabled);
            } catch (SecurityException exception) {
                throw new ChangeSetException(exception.getMessage());
            }
        }
        stored.state = State.APPLIED;
        stored.history.add(audit(actor, "applied", request.reason(), stored.manifestHash()));
        return dto(stored);
    }

    public WorkbenchChangeSetDto rollback(String projectId, String id, Path root, WorkbenchChangeSetDto.ActionRequest request,
                                          String actor, boolean devWriteEnabled) throws IOException {
        StoredChangeSet stored = find(projectId, id);
        require(stored.state == State.APPLIED, "Only applied change sets can be rolled back");
        require(ROLLBACK_CONFIRMATION.equals(request == null ? null : request.confirmationPhrase()),
                "Rollback requires the exact confirmation phrase");
        for (StoredFileChange file : stored.files) {
            try {
                adapter.write(root, file.path, file.before, devWriteEnabled);
            } catch (SecurityException exception) {
                throw new ChangeSetException(exception.getMessage());
            }
        }
        stored.state = State.ROLLED_BACK;
        stored.history.add(audit(actor, "rolled-back", request.reason(), stored.manifestHash()));
        return dto(stored);
    }

    private StoredChangeSet find(String projectId, String id) {
        StoredChangeSet stored = changeSets.get(id);
        if (stored == null || !stored.projectId.equals(projectId)) throw new ChangeSetException("Change set not found");
        return stored;
    }

    private WorkbenchChangeSetDto dto(StoredChangeSet stored) {
        return new WorkbenchChangeSetDto(stored.id, stored.projectId, stored.title, stored.state.name().toLowerCase().replace('_', '-'),
                stored.dangerous, stored.manifestHash(), stored.approvedManifestHash,
                stored.files.stream().map(file -> new WorkbenchChangeSetDto.FileChange(file.path, file.action,
                        sha(file.before), sha(file.after), file.after)).toList(),
                List.copyOf(stored.decisions), List.copyOf(stored.evidence), diffDto(stored), List.copyOf(stored.history));
    }

    private WorkbenchChangeSetDto.Diff diffDto(StoredChangeSet stored) {
        List<WorkbenchChangeSetDto.FileDiff> files = stored.files.stream()
                .map(file -> new WorkbenchChangeSetDto.FileDiff(file.path, file.action, sha(file.before), sha(file.after),
                        preview(file.before, file.after))).toList();
        return new WorkbenchChangeSetDto.Diff(files.size() + " file change(s); manifest " + stored.manifestHash(),
                List.of("diff-viewed", "human-approval", "manifest-match", "role-can-modify"), files);
    }

    private static WorkbenchChangeSetDto.AuditEvent audit(String actor, String action, String reason, String manifestHash) {
        return new WorkbenchChangeSetDto.AuditEvent(actor == null || actor.isBlank() ? "unknown" : actor,
                action, Instant.now().toString(), reason == null ? "" : reason, manifestHash);
    }

    private static List<String> safeList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String readOrEmpty(Path root, String path) throws IOException {
        try {
            return java.nio.file.Files.readString(root.resolve(path).normalize());
        } catch (NoSuchFileException exception) {
            return "";
        }
    }

    private static String preview(String before, String after) {
        return "--- before\n" + truncate(before) + "\n+++ after\n" + truncate(after);
    }

    private static String truncate(String value) {
        return value.length() <= 280 ? value : value.substring(0, 280) + "\n...";
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new ChangeSetException(message);
    }

    private static String sha(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((value == null ? "" : value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder("sha256:");
            for (byte b : bytes) builder.append(String.format("%02x", b));
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash content", exception);
        }
    }

    private enum State { DRAFT, REVIEW, APPROVED, APPLIED, REJECTED, ROLLED_BACK }

    private static final class StoredChangeSet {
        final String id;
        final String projectId;
        final String title;
        State state;
        final boolean dangerous;
        final List<StoredFileChange> files;
        final List<String> decisions;
        final List<String> evidence;
        final Instant createdAt;
        boolean diffViewed;
        String approvedManifestHash;
        final List<WorkbenchChangeSetDto.AuditEvent> history = new ArrayList<>();

        StoredChangeSet(String id, String projectId, String title, State state, boolean dangerous,
                        List<StoredFileChange> files, List<String> decisions, List<String> evidence, Instant createdAt) {
            this.id = id;
            this.projectId = projectId;
            this.title = title;
            this.state = state;
            this.dangerous = dangerous;
            this.files = List.copyOf(files);
            this.decisions = decisions;
            this.evidence = evidence;
            this.createdAt = createdAt;
        }

        String manifestHash() {
            String manifest = projectId + "|" + title + "|" + dangerous + "|" +
                    files.stream().map(file -> file.path + ":" + file.action + ":" + sha(file.after)).sorted()
                            .reduce("", (left, right) -> left + "|" + right) +
                    "|" + String.join(",", decisions) + "|" + String.join(",", evidence);
            return sha(manifest);
        }
    }

    private record StoredFileChange(String path, String action, String before, String after) { }

    public static class ChangeSetException extends RuntimeException {
        public ChangeSetException(String message) {
            super(message);
        }
    }
}
