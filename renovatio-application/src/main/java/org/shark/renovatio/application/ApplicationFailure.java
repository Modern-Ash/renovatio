package org.shark.renovatio.application;

public sealed class ApplicationFailure extends RuntimeException permits ApplicationFailure.NotFound,
        ApplicationFailure.StaleSource, ApplicationFailure.StaleManifest, ApplicationFailure.IdempotencyConflict,
        ApplicationFailure.ValidationFailed, ApplicationFailure.ApplyReverted, ApplicationFailure.CapabilityUnavailable {
    protected ApplicationFailure(String message) { super(message); }
    public static final class NotFound extends ApplicationFailure { public NotFound(String message) { super(message); } }
    public static final class StaleSource extends ApplicationFailure { public StaleSource(String message) { super(message); } }
    public static final class StaleManifest extends ApplicationFailure { public StaleManifest(String message) { super(message); } }
    public static final class IdempotencyConflict extends ApplicationFailure { public IdempotencyConflict(String message) { super(message); } }
    public static final class ValidationFailed extends ApplicationFailure { public ValidationFailed(String message) { super(message); } }
    public static final class ApplyReverted extends ApplicationFailure {
        private final String changeSetId;
        public ApplyReverted(String changeSetId, Throwable cause) { super("apply reverted: " + changeSetId); this.changeSetId = changeSetId; initCause(cause); }
        public String changeSetId() { return changeSetId; }
    }
    public static final class CapabilityUnavailable extends ApplicationFailure { public CapabilityUnavailable(String message) { super(message); } }
}
