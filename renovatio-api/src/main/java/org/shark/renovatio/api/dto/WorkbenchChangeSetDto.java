package org.shark.renovatio.api.dto;

import java.util.List;

public record WorkbenchChangeSetDto(String id,
                                    String projectId,
                                    String title,
                                    String state,
                                    boolean dangerous,
                                    String manifestHash,
                                    String approvedManifestHash,
                                    List<FileChange> files,
                                    List<String> decisions,
                                    List<String> evidence,
                                    Diff diff,
                                    List<AuditEvent> history) {
    public record FileChange(String path,
                             String action,
                             String beforeHash,
                             String afterHash,
                             String proposedContent) { }

    public record Diff(String summary,
                       List<String> requiredBeforeApproval,
                       List<FileDiff> files) { }

    public record FileDiff(String path,
                           String action,
                           String beforeHash,
                           String afterHash,
                           String preview) { }

    public record AuditEvent(String actor,
                             String action,
                             String at,
                             String reason,
                             String manifestHash) { }

    public record CreateRequest(String title,
                                boolean dangerous,
                                List<FileChangeRequest> files,
                                List<String> decisions,
                                List<String> evidence) { }

    public record FileChangeRequest(String path,
                                    String action,
                                    String proposedContent) { }

    public record ApprovalRequest(boolean diffReviewed,
                                  String confirmationPhrase,
                                  String reason) { }

    public record ActionRequest(String confirmationPhrase,
                                String reason) { }
}
