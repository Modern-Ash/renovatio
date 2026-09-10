package org.shark.renovatio.llm.governance;

import org.shark.renovatio.llm.domain.LLMConfig;
import org.shark.renovatio.llm.domain.ProposalMetadata;
import org.shark.renovatio.llm.domain.ProposalRequest;
import org.shark.renovatio.llm.domain.TypedProposal;
import org.shark.renovatio.llm.security.GovernanceRedactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Governance logger for LLM calls.
 * Records prompt/schema/model/version/input hash/output hash/cache status/actor and human decision.
 * Does not persist secrets.
 */
@Component
public class GovernanceLogger {

    private static final Logger logger = LoggerFactory.getLogger(GovernanceLogger.class);
    private final GovernanceRedactor redactor;
    private final Path auditLogPath;
    private final Map<String, GovernanceEntry> pendingDecisions = new ConcurrentHashMap<>();

    public GovernanceLogger(GovernanceRedactor redactor) {
        this.redactor = redactor;
        this.auditLogPath = Path.of("renovatio-llm/audit");
        try {
            Files.createDirectories(auditLogPath);
        } catch (IOException e) {
            logger.error("Failed to create audit log directory", e);
        }
    }

    /**
     * Logs an LLM call with all governance metadata.
     */
    public String logCall(
            String actor,
            ProposalRequest request,
            TypedProposal proposal,
            LLMConfig config,
            long latencyMs,
            String cacheStatus) {

        String decisionId = UUID.randomUUID().toString();
        String promptHash = hash(request.toString());
        String inputHash = request.input().sourceHash();
        String outputHash = hash(proposal.toString());

        GovernanceEntry entry = new GovernanceEntry(
            decisionId,
            Instant.now(),
            actor,
            request.kind(),
            request.schema(),
            config.model(),
            config.provider(),
            promptHash,
            inputHash,
            outputHash,
            proposal.metadata().tokensUsed(),
            proposal.metadata().latencyMs(),
            cacheStatus,
            proposal.confidence(),
            proposal.rationale(),
            redactor.redactForLog(request.input().context())
        );

        // Store for potential human review
        pendingDecisions.put(decisionId, entry);

        // Write to audit log
        writeAuditLog(entry);

        logger.info("LLM call logged: decisionId={}, kind={}, confidence={}, actor={}",
            decisionId, request.kind(), proposal.confidence(), actor);

        return decisionId;
    }

    /**
     * Records a human decision on a proposal.
     */
    public void recordDecision(String decisionId, String actor, Decision decision, String rationale) {
        GovernanceEntry entry = pendingDecisions.get(decisionId);
        if (entry == null) {
            logger.warn("No pending decision found for decisionId={}", decisionId);
            return;
        }

        entry.setDecision(decision);
        entry.setDecisionActor(actor);
        entry.setDecisionRationaleHash(rationale != null ? hash(rationale) : null);
        entry.setDecisionTimestamp(Instant.now());

        writeAuditLog(entry);
        pendingDecisions.remove(decisionId);

        logger.info("Decision recorded: decisionId={}, decision={}, actor={}",
            decisionId, decision, actor);
    }

    /**
     * Gets a pending decision for review.
     */
    public GovernanceEntry getPendingDecision(String decisionId) {
        return pendingDecisions.get(decisionId);
    }

    private void writeAuditLog(GovernanceEntry entry) {
        try {
            String logLine = entry.toJsonLine();
            Files.writeString(
                auditLogPath.resolve("governance-" + Instant.now().toString().substring(0, 10) + ".log"),
                logLine + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            logger.error("Failed to write audit log", e);
        }
    }

    private String hash(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return "sha256:" + hex.toString();
        } catch (Exception e) {
            return "hash-error";
        }
    }

    /**
     * Governance log entry.
     */
    public static class GovernanceEntry {
        private final String decisionId;
        private final Instant timestamp;
        private final String actor;
        private final String kind;
        private final String schema;
        private final String model;
        private final String provider;
        private final String promptHash;
        private final String inputHash;
        private final String outputHash;
        private final int tokensUsed;
        private final long latencyMs;
        private final String cacheStatus;
        private final double confidence;
        private final String rationale;
        private final Map<String, Object> redactedContext;
        private Decision decision;
        private String decisionActor;
        private String decisionRationaleHash;
        private Instant decisionTimestamp;

        public GovernanceEntry(
                String decisionId,
                Instant timestamp,
                String actor,
                String kind,
                String schema,
                String model,
                String provider,
                String promptHash,
                String inputHash,
                String outputHash,
                int tokensUsed,
                long latencyMs,
                String cacheStatus,
                double confidence,
                String rationale,
                Map<String, Object> redactedContext) {
            this.decisionId = decisionId;
            this.timestamp = timestamp;
            this.actor = actor;
            this.kind = kind;
            this.schema = schema;
            this.model = model;
            this.provider = provider;
            this.promptHash = promptHash;
            this.inputHash = inputHash;
            this.outputHash = outputHash;
            this.tokensUsed = tokensUsed;
            this.latencyMs = latencyMs;
            this.cacheStatus = cacheStatus;
            this.confidence = confidence;
            this.rationale = rationale;
            this.redactedContext = redactedContext;
        }

        // Getters and setters
        public String getDecisionId() { return decisionId; }
        public Instant getTimestamp() { return timestamp; }
        public String getActor() { return actor; }
        public String getKind() { return kind; }
        public String getSchema() { return schema; }
        public String getModel() { return model; }
        public String getProvider() { return provider; }
        public String getPromptHash() { return promptHash; }
        public String getInputHash() { return inputHash; }
        public String getOutputHash() { return outputHash; }
        public int getTokensUsed() { return tokensUsed; }
        public long getLatencyMs() { return latencyMs; }
        public String getCacheStatus() { return cacheStatus; }
        public double getConfidence() { return confidence; }
        public String getRationale() { return rationale; }
        public Map<String, Object> getRedactedContext() { return redactedContext; }
        public Decision getDecision() { return decision; }
        public void setDecision(Decision decision) { this.decision = decision; }
        public String getDecisionActor() { return decisionActor; }
        public void setDecisionActor(String decisionActor) { this.decisionActor = decisionActor; }
        public String getDecisionRationaleHash() { return decisionRationaleHash; }
        public void setDecisionRationaleHash(String decisionRationaleHash) { this.decisionRationaleHash = decisionRationaleHash; }
        public Instant getDecisionTimestamp() { return decisionTimestamp; }
        public void setDecisionTimestamp(Instant decisionTimestamp) { this.decisionTimestamp = decisionTimestamp; }

        public String toJsonLine() {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(this);
            } catch (Exception e) {
                return "{\"error\":\"json serialization failed\"}";
            }
        }
    }

    public enum Decision {
        ACCEPT,
        REJECT,
        FALLBACK
    }
}