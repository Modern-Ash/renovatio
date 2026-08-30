package org.shark.renovatio.llm.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Verifies manifest claims against immutable Git objects and committed Agora owner evidence. */
public final class GovernedPromotionVerifier {
    private static final String APPROVALS_PATH = ".agora/swarms/002-ai-modernization/work/"
            + "llm-runtime-catalog-cache/approvals.md";
    private static final String INDEX_PATH = CommittedCacheArtifactsLoader.INDEX_PATH;
    private final ObjectMapper json = new ObjectMapper();
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    public void verify(PromotionRepository repository, CommittedCacheArtifacts authority) {
        for (var item : authority.manifest().entries().entrySet()) {
            String key = item.getKey();
            VerifiedPromotionManifest.Entry proof = item.getValue();
            String envelopePath = CommittedCacheIndexGenerator.CACHE_PREFIX + proof.repositoryPath();
            require(repository.isAncestor(proof.commitA(), proof.commitB()));
            require(repository.isAncestor(proof.commitB(), proof.commitC()));
            require(repository.isAncestor(proof.commitC(), repository.head()));
            require(repository.changedPaths(proof.commitA()).contains(envelopePath));
            require(repository.changedPaths(proof.commitB()).contains(INDEX_PATH));

            byte[] envelopeBytes = repository.read(proof.commitA(), envelopePath);
            require(CacheKey.sha256(envelopeBytes).equals(proof.contentHash()));
            try {
                CacheEnvelope envelope = json.readValue(envelopeBytes, CacheEnvelope.class);
                require(envelope.cacheKey().equals(key));
                require(envelope.envelopeHash().equals(proof.envelopeHash()));
                require(envelope.promotionDisposition() == PromotionDisposition.COMMITTED);
                require(envelope.hasValidHash());
                verifyAttribution(repository, proof.commitC(), envelope);
                CommittedCacheIndex indexAtB = json.readValue(repository.read(proof.commitB(), INDEX_PATH),
                        CommittedCacheIndex.class);
                require(indexAtB.equals(authority.index()));
            } catch (IOException exception) {
                throw new IllegalStateException("CACHE_PROMOTION_OBJECT_INVALID", exception);
            }

            String evidencePath = repositoryPath(proof.approvalEvidenceRef());
            require(repository.changedPaths(proof.commitC()).contains(evidencePath));
            require(repository.changedPaths(proof.commitC()).contains(APPROVALS_PATH));
            JsonNode evidence = frontMatter(repository.read(proof.commitC(), evidencePath));
            require("cache-promotion".equals(evidence.path("type").asText()));
            require("success".equals(evidence.path("result").asText()));
            require("project:owner".equals(evidence.path("produced-by").asText()));
            require(proof.commitB().equals(evidence.path("tested-commit").asText()));
            JsonNode references = evidence.path("artifact-references");
            require(references.isArray() && references.size() == 1);
            String reportUri = references.get(0).asText();
            String reportPath = repositoryPath(reportUri);
            String expectedDigest = evidence.path("artifact-content-sha256").path(reportUri).asText();
            require(CacheKey.sha256(repository.read(proof.commitC(), reportPath)).equals(expectedDigest));

            String approvals = new String(repository.read(proof.commitC(), APPROVALS_PATH),
                    StandardCharsets.UTF_8);
            require(approvals.contains("| spec-owner | project:owner |"));
            require(approvals.contains(proof.commitA()));
            require(approvals.contains(proof.commitB()));
            require(approvals.contains(proof.envelopeHash()));
            require(approvals.contains(authority.index().digest()));
        }
    }

    private void verifyAttribution(PromotionRepository repository, String commit, CacheEnvelope envelope) {
        String run = envelope.governedRunReference();
        require(run.matches("tool-[0-9]{8}t[0-9]{14}z"));
        JsonNode invocation = frontMatter(repository.read(commit, ".agora/tool-runs/" + run + "/RUN.md"));
        JsonNode resultRecord = frontMatter(repository.read(commit,
                ".agora/tool-runs/" + run + "/RESULT.md"));
        require("llm-enrichment".equals(invocation.path("tool").asText()));
        require("enrich".equals(invocation.path("operation").asText()));
        require("completed".equals(invocation.path("status").asText()));
        require(invocation.path("exit-code").asInt(-1) == 0);
        require(run.equals(resultRecord.path("run").asText()));
        require("completed".equals(resultRecord.path("status").asText()));
        require(resultRecord.path("exit-code").asInt(-1) == 0);

        JsonNode inputs = invocation.path("inputs");
        require(envelope.promptId().equals(inputs.path("prompt-id").asText()));
        require(envelope.provider().equals(inputs.path("provider").asText()));
        require(envelope.model().equals(inputs.path("model").asText()));
        require(envelope.inputHash().equals(inputs.path("input-hash").asText()));
        require(envelope.cacheKey().equals(inputs.path("cache-key").asText()));
        require(envelope.outputSchemaHash().equals(inputs.path("schema-hash").asText()));
        require(envelope.runtimeContractVersion().equals(
                inputs.path("runtime-contract-version").asText()));

        JsonNode result = resultPayload(repository.read(commit,
                ".agora/tool-runs/" + run + "/RESULT.md"));
        require(envelope.outputHash().equals(result.path("outputHash").asText()));
        require(envelope.resultDisposition().name().equals(result.path("resultDisposition").asText()));
        require("PENDING_PROMOTION".equals(result.path("promotionDisposition").asText()));
        require(envelope.artifactUri().equals(result.path("artifactUri").asText()));
        require(envelope.outputSchemaHash().equals(result.path("schemaHash").asText()));
        require(envelope.cacheKey().equals(result.path("cacheKey").asText()));
        require(envelope.runtimeContractVersion().equals(
                result.path("runtimeContractVersion").asText()));
    }

    private JsonNode resultPayload(byte[] documentBytes) {
        String document = new String(documentBytes, StandardCharsets.UTF_8);
        for (String line : document.split("\\R")) {
            String candidate = line.strip();
            if (candidate.startsWith("{")) {
                try {
                    return json.readTree(candidate);
                } catch (IOException exception) {
                    throw new IllegalStateException("CACHE_ATTRIBUTION_RESULT_INVALID", exception);
                }
            }
        }
        throw new IllegalStateException("CACHE_ATTRIBUTION_RESULT_MISSING");
    }

    private JsonNode frontMatter(byte[] documentBytes) {
        String document = new String(documentBytes, StandardCharsets.UTF_8);
        int close = document.indexOf("\n---", 4);
        if (!document.startsWith("---\n") || close < 0) {
            throw new IllegalStateException("CACHE_PROMOTION_EVIDENCE_INVALID");
        }
        try {
            return yaml.readTree(document.substring(4, close));
        } catch (IOException exception) {
            throw new IllegalStateException("CACHE_PROMOTION_EVIDENCE_INVALID", exception);
        }
    }

    private static String repositoryPath(String uri) {
        if (uri == null || !uri.startsWith("repo://") || uri.substring(7).contains("..")) {
            throw new IllegalStateException("CACHE_PROMOTION_REFERENCE_INVALID");
        }
        return uri.substring(7);
    }

    private static void require(boolean condition) {
        if (!condition) throw new IllegalStateException("CACHE_PROMOTION_VERIFICATION_FAILED");
    }
}
